package no.nav.doknotifikasjon.knot003;

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

import java.time.LocalDateTime;
import java.util.Optional;

import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;
import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_SHORT;
import static no.nav.doknotifikasjon.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.doknotifikasjon.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_DATABASE_IKKE_OPPDATERT;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_EPOST_UGYLDIG_KANAL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_EPOST_UGYLDIG_STATUS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_EPOST;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_SMS;

@Slf4j
@Component
public class Knot003Service {

    private final Knot003Mapper knot003Mapper;
    private final KafkaEventProducer kafkaEventProducer;
    private final AltinnVarselConsumer altinnVarselConsumer;
    private final MetricService metricService;
    private final NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

    public Knot003Service(Knot003Mapper knot003Mapper, KafkaEventProducer kafkaEventProducer,
                          AltinnVarselConsumer altinnVarselConsumer, MetricService metricService,
                          NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository) {
        this.kafkaEventProducer = kafkaEventProducer;
        this.altinnVarselConsumer = altinnVarselConsumer;
        this.metricService = metricService;
        this.knot003Mapper = knot003Mapper;
        this.notifikasjonDistribusjonRepository = notifikasjonDistribusjonRepository;
    }

    public void shouldSendEpost(int notifikasjonDistribusjonId) {
        log.info("Ny hendelse med notifikasjonsDistribusjonId={} på kafka-topic {} hentet av knot003.", notifikasjonDistribusjonId, KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST);

        NotifikasjonDistribusjon notifikasjonDistribusjon = queryRepository(notifikasjonDistribusjonId);
        Notifikasjon notifikasjon = notifikasjonDistribusjon.getNotifikasjon();

        DoknotifikasjonEpostObject doknotifikasjonEpostObject = knot003Mapper.mapNotifikasjonDistrubisjon(notifikasjonDistribusjon, notifikasjon);

        if (!validateDistribusjonStatusOgKanal(doknotifikasjonEpostObject)) {
            String melding = doknotifikasjonEpostObject.getDistribusjonStatus() == Status.OPPRETTET ? FEILET_EPOST_UGYLDIG_KANAL : FEILET_EPOST_UGYLDIG_STATUS;
            publishStatus(doknotifikasjonEpostObject, Status.FEILET, melding);
            log.warn("Behandling av melding på kafka-topic={} avsluttes pga feil={}", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST, melding);
            return;
        }

        try {
            altinnVarselConsumer.sendVarsel(Kanal.EPOST, doknotifikasjonEpostObject.getKontaktInfo(), doknotifikasjonEpostObject.getFodselsnummer(), doknotifikasjonEpostObject.getTekst(), "");
            log.info(FERDIGSTILT_NOTIFIKASJON_SMS + " notifikasjonDistribusjonId={}", notifikasjonDistribusjonId);
        } catch (AltinnFunctionalException altinnFunctionalException) {
            log.error("Knot003 NotifikasjonDistribusjonConsumer funksjonell feil ved kall mot altinn: feilmelding={}", altinnFunctionalException.getMessage(), altinnFunctionalException);
            publishStatus(doknotifikasjonEpostObject, Status.FEILET, altinnFunctionalException.getMessage());
            return;
        } catch (Exception unknownException) {
            log.error("Knot003 NotifikasjonDistribusjonConsumer ukjent exception", unknownException);
            publishStatus(doknotifikasjonEpostObject, Status.FEILET, Optional.of(unknownException).map(Exception::getMessage).orElse(""));
            return;
        }

        updateEntity(notifikasjonDistribusjon, notifikasjon.getBestillerId());  //todo
        // Uendelig retry ved databasefeil
        // Alexander-Haugli:
        // "og så har vi tenkt "uendelig" retry med overvåkning på grafana dashboard.
        // ikke helt overbevist om at det er optimalt, men hvis altinn eller db gir tekniske feil,
        // så har det ikke så mye for seg å gå videre (ingen grunn til å tro at det går bedre med neste melding)
        // - så at vi får en "propp" i behandlingen er kanskje ikke så feil"

        publishStatus(doknotifikasjonEpostObject, Status.FERDIGSTILT, FERDIGSTILT_NOTIFIKASJON_EPOST);
        metricService.metricKnot003EpostProcessed();
    }

    private boolean validateDistribusjonStatusOgKanal(DoknotifikasjonEpostObject doknotifikasjonEpostObject) {
        return Status.OPPRETTET.equals(doknotifikasjonEpostObject.getDistribusjonStatus()) && Kanal.EPOST.equals(doknotifikasjonEpostObject.getKanal());
    }

    private void publishStatus(DoknotifikasjonEpostObject doknotifikasjonEpostObject, Status status, String melding) {
        kafkaEventProducer.publish(
                KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
                String.valueOf(doknotifikasjonEpostObject.getNotifikasjonDistribusjonId()),
                DoknotifikasjonStatus.newBuilder()
                        .setBestillerId(doknotifikasjonEpostObject.getBestillerId())
                        .setBestillingsId(doknotifikasjonEpostObject.getBestillingsId())
                        .setStatus(status.name())
                        .setMelding(melding)
                        .setDistribusjonId(Long.valueOf(doknotifikasjonEpostObject.getNotifikasjonDistribusjonId()))
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