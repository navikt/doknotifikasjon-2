package no.nav.doknotifikasjon.knot002.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.consumer.altinn.AltinnConsumer;
import no.nav.doknotifikasjon.knot002.domain.DoknotifikasjonSms;
import no.nav.doknotifikasjon.knot002.mapper.NotifikasjonEntityMapper;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.doknotifikasjon.kafka.KafkaTopics;
import org.springframework.stereotype.Component;
import org.springframework.ws.soap.client.SoapFaultClientException;


@Slf4j
@Component
public class NotifikasjonDistribusjonConsumer {

    private final NotifikasjonEntityMapper notifikasjonEntityMapper;
    private final KafkaEventProducer kafkaEventProducer;
    private final AltinnConsumer altinnConsumer;

    private final String FERDIGSTILLT = "notifikasjon sendt via sms";
    private final String UGYLDIG_STATUS = "distribusjon til sms feilet: ugyldig status";
    private final String UGYLDIG_KANAL = "distribusjon til sms feilet: ugyldig kanal";
    private final String IKKE_OPPDATERT = "Oppdatering av distrubusjon feilet i database";

    public NotifikasjonDistribusjonConsumer(
            NotifikasjonEntityMapper notifikasjonEntityMapper,
            KafkaEventProducer kafkaEventProducer,
            AltinnConsumer altinnConsumer
    ){
        this.notifikasjonEntityMapper = notifikasjonEntityMapper;
        this.kafkaEventProducer = kafkaEventProducer;
        this.altinnConsumer = altinnConsumer;
    }

    public void konsumerDistribusjonId(String notifikasjonDistribusjonId){

        DoknotifikasjonSms doknotifikasjonSms;

        try{
            doknotifikasjonSms = notifikasjonEntityMapper.mapNotifikasjon(notifikasjonDistribusjonId);
        } catch(Exception exception) {
            log.error("NotifikasjonDistribusjonConsumer kunne ikke hente notifikasjon", exception);
            return;
        }

        if(!validateDistribusjonStatusOgKanal(doknotifikasjonSms)){
            String melding = doknotifikasjonSms.distribusjonStatus == Status.OPPRETTET
                    ? UGYLDIG_STATUS
                    : UGYLDIG_KANAL;

            publishStatus(doknotifikasjonSms, Status.FEILET, melding);
            return;
        }

        try{
            altinnConsumer.sendStandaloneNotificationV3(
                    Kanal.SMS,
                    doknotifikasjonSms.kontakt,
                    doknotifikasjonSms.tekst
            );

            log.info("NotifikasjonDistribusjonConsumer" + FERDIGSTILLT + " notifikasjonDistribusjonId=${}", notifikasjonDistribusjonId);
        } catch (SoapFaultClientException soapFault) {
            log.error("NotifikasjonDistribusjonConsumer soapfault: fault reason=${}",soapFault.getFaultStringOrReason(), soapFault);
            publishStatus(doknotifikasjonSms, Status.FEILET, soapFault.getFaultStringOrReason());
            return;
        } catch (Exception altinnException) {
            log.error("NotifikasjonDistribusjonConsumer annen exception:", altinnException);
            publishStatus(doknotifikasjonSms, Status.FEILET, altinnException.getMessage());
            return;
        }


        try{
            notifikasjonEntityMapper.updateEntity(
                    notifikasjonDistribusjonId,
                    doknotifikasjonSms.bestillerId
            );
        } catch(Exception exception) {
            log.error(IKKE_OPPDATERT, exception);
            // TODO feilhåndtering?
            return;
        }

        publishStatus(doknotifikasjonSms, Status.FERDIGSTILT, FERDIGSTILLT);

    }

    private boolean validateDistribusjonStatusOgKanal(DoknotifikasjonSms doknotifikasjonSms){
        return doknotifikasjonSms.getDistribusjonStatus().equals(Status.OPPRETTET)
                && doknotifikasjonSms.kanal.equals(Kanal.SMS);
    }

    private void publishStatus(DoknotifikasjonSms doknotifikasjonSms, Status status, String melding) {
        kafkaEventProducer.publish(
                KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
                doknotifikasjonSms.notifikasjonDistribusjonId,
                DoknotifikasjonStatus.newBuilder()
                        .setBestillerId(doknotifikasjonSms.bestillerId)
                        .setBestillingsId(doknotifikasjonSms.bestillingsId)
                        .setStatus(status.name())
                        .setMelding(melding)
                        .setDistribusjonId(Long.valueOf(doknotifikasjonSms.getNotifikasjonDistribusjonId()))
                        .build(),
                System.currentTimeMillis()
        );
    }

}
