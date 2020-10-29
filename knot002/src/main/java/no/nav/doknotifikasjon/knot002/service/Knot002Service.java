package no.nav.doknotifikasjon.knot002.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
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

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILLT;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.UGYLDIG_STATUS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.UGYLDIG_KANAL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.IKKE_OPPDATERT;

@Slf4j
@Component
public class Knot002Service {

    private final NotifikasjonEntityMapper notifikasjonEntityMapper;
    private final KafkaEventProducer kafkaEventProducer;
    private final AltinnConsumer altinnConsumer;

    public Knot002Service(
            NotifikasjonEntityMapper notifikasjonEntityMapper,
            KafkaEventProducer kafkaEventProducer,
            AltinnConsumer altinnConsumer
    ) {
        this.notifikasjonEntityMapper = notifikasjonEntityMapper;
        this.kafkaEventProducer = kafkaEventProducer;
        this.altinnConsumer = altinnConsumer;
    }

    public void konsumerDistribusjonId(int notifikasjonDistribusjonId) {

        DoknotifikasjonSms doknotifikasjonSms;

        try{
            doknotifikasjonSms = notifikasjonEntityMapper.mapNotifikasjonDistrubisjon(notifikasjonDistribusjonId);
        } catch(Exception exception) {
            log.error("NotifikasjonDistribusjonConsumer kunne ikke hente notifikasjon", exception);
            return;
        }

        if(!validateDistribusjonStatusOgKanal(doknotifikasjonSms)) {
            String melding = doknotifikasjonSms.distribusjonStatus == Status.OPPRETTET
                    ? UGYLDIG_KANAL
                    : UGYLDIG_STATUS;

            publishStatus(doknotifikasjonSms, Status.FEILET, melding);
            return;
        }

        try{
            altinnConsumer.sendStandaloneNotificationV3(
                    Kanal.SMS,
                    doknotifikasjonSms.kontakt,
                    doknotifikasjonSms.tekst
            );

            log.info("Knot002 NotifikasjonDistribusjonConsumer" + FERDIGSTILLT + " notifikasjonDistribusjonId=${}", notifikasjonDistribusjonId);
        } catch (SoapFaultClientException soapFault) {
            log.error("Knot002 NotifikasjonDistribusjonConsumer soapfault: fault reason=${}", soapFault.getFaultStringOrReason(), soapFault);
            publishStatus(doknotifikasjonSms, Status.FEILET, soapFault.getFaultStringOrReason());
            return;

        } catch (AltinnFunctionalException altinnFunctionalException) {
            log.error("Knot002 NotifikasjonDistribusjonConsumer funksjonell feil ved kall mot altinn: fault reason=${}", altinnFunctionalException.getMessage(), altinnFunctionalException);
            publishStatus(doknotifikasjonSms, Status.FEILET, altinnFunctionalException.getMessage());
            return;
        } catch (Exception altinnException) {
            log.error("Knot002 NotifikasjonDistribusjonConsumer annen exception:", altinnException);
            // TODO udefinert spec for teknisk feil mot altinn, ANNTAR at behandling skal avsluttes og status legges på kø.
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
            // TODO feilhåndtering? Udefinert i spec
            return;
        }

        publishStatus(doknotifikasjonSms, Status.FERDIGSTILT, FERDIGSTILLT);

    }

    private boolean validateDistribusjonStatusOgKanal(DoknotifikasjonSms doknotifikasjonSms) {
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
