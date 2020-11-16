package no.nav.doknotifikasjon.knot002;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.consumer.altinn.AltinnVarselConsumer;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
import no.nav.doknotifikasjon.exception.technical.DoknotifikasjonDBTechnicalException;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaTopics;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Optional;

import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;
import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_SHORT;
import static no.nav.doknotifikasjon.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.doknotifikasjon.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_DATABASE_IKKE_OPPDATERT;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SMS_UGYLDIG_KANAL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SMS_UGYLDIG_STATUS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_SMS;

@Slf4j
@Component
public class Knot002Service {

    private final Knot002Mapper knot002Mapper;
    private final MetricService metricService;
    private final KafkaEventProducer kafkaEventProducer;
    private final AltinnVarselConsumer altinnVarselConsumer;
    private final NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

    @Inject
    public Knot002Service(Knot002Mapper knot002Mapper, KafkaEventProducer kafkaEventProducer,
                          AltinnVarselConsumer altinnVarselConsumer, NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository, MetricService metricService) {
        this.knot002Mapper = knot002Mapper;
        this.kafkaEventProducer = kafkaEventProducer;
        this.altinnVarselConsumer = altinnVarselConsumer;
        this.notifikasjonDistribusjonRepository = notifikasjonDistribusjonRepository;
        this.metricService = metricService;
    }

    public void shouldSendSms(int notifikasjonDistribusjonId) {
        log.info("Ny hendelse med notifikasjonsDistribusjonId={} på kafka-topic {} hentet av knot002.", notifikasjonDistribusjonId, KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS);

        NotifikasjonDistribusjon notifikasjonDistribusjon = queryRepository(notifikasjonDistribusjonId);
        Notifikasjon notifikasjon = notifikasjonDistribusjon.getNotifikasjon();

        DoknotifikasjonSmsObject doknotifikasjonSmsObject = knot002Mapper.mapNotifikasjonDistrubisjon(notifikasjonDistribusjon, notifikasjon);

        if (!validateDistribusjonStatusOgKanal(doknotifikasjonSmsObject)) {
            String melding = doknotifikasjonSmsObject.getDistribusjonStatus() == Status.OPPRETTET ? FEILET_SMS_UGYLDIG_KANAL : FEILET_SMS_UGYLDIG_STATUS;
            publishStatus(doknotifikasjonSmsObject, Status.FEILET, melding);
            log.warn("Behandling av melding på kafka-topic={} avsluttes pga feil={}", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS, melding);
            return;
        }

        try {
            altinnVarselConsumer.sendVarsel(Kanal.SMS, doknotifikasjonSmsObject.getKontaktInfo(), doknotifikasjonSmsObject.getFodselsnummer(), doknotifikasjonSmsObject.getTekst(), "");
            log.info(FERDIGSTILT_NOTIFIKASJON_SMS + " notifikasjonDistribusjonId={}", notifikasjonDistribusjonId);
        } catch (AltinnFunctionalException altinnFunctionalException) {
            log.error("Knot002 NotifikasjonDistribusjonConsumer funksjonell feil ved kall mot altinn: feilmelding={}", altinnFunctionalException.getMessage(), altinnFunctionalException);
            publishStatus(doknotifikasjonSmsObject, Status.FEILET, altinnFunctionalException.getMessage());
            metricService.metricHandleException(altinnFunctionalException);
            return;
        } catch (Exception unknownException) {
            log.error("Knot002 NotifikasjonDistribusjonConsumer ukjent exception", unknownException);
            publishStatus(doknotifikasjonSmsObject, Status.FEILET, Optional.of(unknownException).map(Exception::getMessage).orElse(""));
            metricService.metricHandleException(unknownException);
            return;
        }

        updateEntity(notifikasjonDistribusjon, notifikasjon.getBestillerId());  //todo
        // Uendelig retry ved databasefeil
        // Alexander-Haugli:
        // "og så har vi tenkt "uendelig" retry med overvåkning på grafana dashboard.
        // ikke helt overbevist om at det er optimalt, men hvis altinn eller db gir tekniske feil,
        // så har det ikke så mye for seg å gå videre (ingen grunn til å tro at det går bedre med neste melding)
        // - så at vi får en "propp" i behandlingen er kanskje ikke så feil"

        publishStatus(doknotifikasjonSmsObject, Status.FERDIGSTILT, FERDIGSTILT_NOTIFIKASJON_SMS);
        metricService.metricKnot002SmsSent();
    }

    private boolean validateDistribusjonStatusOgKanal(DoknotifikasjonSmsObject doknotifikasjonSmsObject) {
        return Status.OPPRETTET.equals(doknotifikasjonSmsObject.getDistribusjonStatus()) && Kanal.SMS.equals(doknotifikasjonSmsObject.getKanal());
    }

    private void publishStatus(DoknotifikasjonSmsObject doknotifikasjonSmsObject, Status status, String melding) {
        kafkaEventProducer.publish(
                KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
                String.valueOf(doknotifikasjonSmsObject.getNotifikasjonDistribusjonId()),
                DoknotifikasjonStatus.newBuilder()
                        .setBestillerId(doknotifikasjonSmsObject.getBestillerId())
                        .setBestillingsId(doknotifikasjonSmsObject.getBestillingsId())
                        .setStatus(status.name())
                        .setMelding(melding)
                        .setDistribusjonId(doknotifikasjonSmsObject.getNotifikasjonDistribusjonId())
                        .build(),
                System.currentTimeMillis()
        );
    }

    @Retryable(include = DoknotifikasjonDistribusjonIkkeFunnetException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
    private NotifikasjonDistribusjon queryRepository(int notifikasjonDistribusjonId) {
        return notifikasjonDistribusjonRepository.findById(notifikasjonDistribusjonId).orElseThrow(
                () -> {
                    throw new DoknotifikasjonDistribusjonIkkeFunnetException(String.format(
                            "NotifikasjonDistribusjon med id=%s ble ikke funnet i databasen.", notifikasjonDistribusjonId)
                    );
                }
        );
    }

    //todo: Uendelig retry
    @Retryable(include = DoknotifikasjonDBTechnicalException.class, backoff = @Backoff(delay = DELAY_LONG, multiplier = MULTIPLIER_SHORT))
    public void updateEntity(NotifikasjonDistribusjon notifikasjonDistribusjon, String bestillerId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            notifikasjonDistribusjon.setEndretAv(bestillerId);
            notifikasjonDistribusjon.setStatus(Status.FERDIGSTILT);
            notifikasjonDistribusjon.setSendtDato(now);
            notifikasjonDistribusjon.setEndretDato(now);

            notifikasjonDistribusjonRepository.save(notifikasjonDistribusjon);
        } catch (Exception e) {
            log.warn(FEILET_DATABASE_IKKE_OPPDATERT, e);
            throw new DoknotifikasjonDBTechnicalException(e.getMessage(), e);
        }
    }
}
