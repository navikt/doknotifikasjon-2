package no.nav.doknotifikasjon.knot002.consumer;

import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.KafkaProducer.KafkaEventProducer;
import no.nav.doknotifikasjon.consumer.altinn.AltinnConsumer;
import no.nav.doknotifikasjon.knot002.domain.DoknotifikasjonSms;
import no.nav.doknotifikasjon.knot002.mapper.NotifikasjonEntityMapper;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.doknotifikasjon.utils.KafkaTopics;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Slf4j
@Component
public class NotifikasjonDistribusjonIdConsumer {

    private final NotifikasjonEntityMapper notifikasjonEntityMapper;
    private final KafkaEventProducer kafkaEventProducer;
    private final AltinnConsumer altinnConsumer;

    private final String FERDIGSTILLT = "notifikasjon sendt via sms";
    private final String UGYLDIG_STATUS = "distribusjon til sms feilet: ugyldig status";
    private final String UGYLDIG_KANAL = "distribusjon til sms feilet: ugyldig kanal";

    NotifikasjonDistribusjonIdConsumer(
            NotifikasjonEntityMapper notifikasjonEntityMapper,
            KafkaEventProducer kafkaEventProducer,
            AltinnConsumer altinnConsumer
    ){
        this.notifikasjonEntityMapper = notifikasjonEntityMapper;
        this.kafkaEventProducer = kafkaEventProducer;
        this.altinnConsumer = altinnConsumer;
    }

    public void konsumerDistribusjonId(String notifikasjonDistribusjonId){
        Either<Throwable, DoknotifikasjonSms> either = notifikasjonEntityMapper.mapNotifikasjon(notifikasjonDistribusjonId);

        if(either.isLeft()){
          log.error("logg error og avslutt", either.getLeft());
          return;
        }

        DoknotifikasjonSms doknotifikasjonSms = either.get();

        if(!validateDistribusjonStatusOgKanal(doknotifikasjonSms)){
            String melding = doknotifikasjonSms.distribusjonStatus == Status.OPPRETTET
                    ? UGYLDIG_STATUS
                    : UGYLDIG_KANAL;

            publishStatus(doknotifikasjonSms, Status.FEILET, melding);
            return;
        }

        Optional<String> altinnError = altinnConsumer.sendStandaloneNotificationV3(
                Kanal.SMS.name(),
                doknotifikasjonSms.kontakt,
                doknotifikasjonSms.tekst
        );

        if(altinnError.isPresent()){
            publishStatus(doknotifikasjonSms, Status.FEILET, altinnError.get());
            return;
        }

        Optional<Throwable> updateError = notifikasjonEntityMapper.updateEntity(
                notifikasjonDistribusjonId,
                doknotifikasjonSms.bestillerId
        );

        if(updateError.isPresent()){
            //TODO error handling
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
                        .setBestillingsId(doknotifikasjonSms.bestillingId)
                        .setStatus(status.name())
                        .setMelding(melding)
                        .setDistribusjonId(Long.valueOf(doknotifikasjonSms.getNotifikasjonDistribusjonId(), 10))
                        .build(),
                System.currentTimeMillis()
        );
    }

}
