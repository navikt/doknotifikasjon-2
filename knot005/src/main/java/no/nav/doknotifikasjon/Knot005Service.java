package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.kafka.KafkaDoknotifikasjonStatusProducer;
import no.nav.doknotifikasjon.kafka.KafkaTopics;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class Knot005Service {

    private final NotifikasjonRepository notifikasjonRepository;
    private final KafkaDoknotifikasjonStatusProducer kafkaDoknotifikasjonStatusProducer;

    public Knot005Service(NotifikasjonRepository notifikasjonRepository,
                          KafkaDoknotifikasjonStatusProducer kafkaDoknotifikasjonStatusProducer) {
        this.notifikasjonRepository = notifikasjonRepository;
        this.kafkaDoknotifikasjonStatusProducer = kafkaDoknotifikasjonStatusProducer;
    }

    public void shouldStopResending(DoknotifikasjonStoppTo doknotifikasjonStoppTo) {
        validateInput(doknotifikasjonStoppTo);

        Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(doknotifikasjonStoppTo.getBestillingsId());

        if (notifikasjon == null) {
            log.warn("Notifikasjon med bestillingsId={} finnes ikke i notifikasjons databasen. Avslutter behandlingen. ",
                    doknotifikasjonStoppTo.getBestillingsId());
        } else if (Status.FERDIGSTILT.equals(notifikasjon.getStatus())) {
            log.warn("Notifikasjon med bestillingsId={} har status={}. Avslutter behandlingen. ",
                    doknotifikasjonStoppTo.getBestillingsId(), Status.FERDIGSTILT);
        } else {
            updateNotifikasjon(notifikasjon, doknotifikasjonStoppTo);
            publishNewDoknotifikasjonStatus(doknotifikasjonStoppTo);
        }
    }

    private void publishNewDoknotifikasjonStatus(DoknotifikasjonStoppTo doknotifikasjonStoppTo) {
        kafkaDoknotifikasjonStatusProducer.publishDoknotikfikasjonStatusFerdigstilt(
                doknotifikasjonStoppTo.getBestillingsId(),
                doknotifikasjonStoppTo.getBestillerId(), "renotifikasjon er stanset", null);
    }

    private void updateNotifikasjon(Notifikasjon notifikasjon, DoknotifikasjonStoppTo doknotifikasjonStoppTo) {
        notifikasjon.setAntallRenotifikasjoner(0);
        notifikasjon.setNesteRenotifikasjonDato(null);
        notifikasjon.setEndretAv(doknotifikasjonStoppTo.getBestillerId());
        notifikasjon.setEndretDato(LocalDateTime.now());
    }

    private void validateInput(DoknotifikasjonStoppTo doknotifikasjonStoppTo) {
        validateField(doknotifikasjonStoppTo.getBestillerId(), "bestillerId");
        validateField(doknotifikasjonStoppTo.getBestillingsId(), "bestillingsId");
    }

    private void validateField(String field, String fieldName) {
        if (field == null || field.trim().isEmpty()) {
            throw new DoknotifikasjonValidationException(String.format("Valideringsfeil i knot005: Hendelse på kafka-topic " +
                    "%s har tom verdi for %s.", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STOPP, fieldName));
        }
    }
}
