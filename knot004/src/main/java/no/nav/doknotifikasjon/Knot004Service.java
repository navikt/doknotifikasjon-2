package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.KafkaProducer.KafkaDoknotifikasjonStatusProducer;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.utils.KafkaTopics;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Component
public class Knot004Service {

    private final NotifikasjonRepository notifikasjonRepository;
    private final DoknotifikasjonStatusValidator doknotifikasjonStatusValidator;
    private final KafkaDoknotifikasjonStatusProducer kafkaDoknotifikasjonStatusProducer;

    public Knot004Service(NotifikasjonRepository notifikasjonRepository, DoknotifikasjonStatusValidator doknotifikasjonStatusValidator,
                          KafkaDoknotifikasjonStatusProducer kafkaDoknotifikasjonStatusProducer) {
        this.notifikasjonRepository = notifikasjonRepository;
        this.doknotifikasjonStatusValidator = doknotifikasjonStatusValidator;
        this.kafkaDoknotifikasjonStatusProducer = kafkaDoknotifikasjonStatusProducer;
    }

    public void shouldUpdateStatus(DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
        log.info("Ny hendelse med bestillingsId={} på kafka-topic {} hentet av knot004.", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
                doknotifikasjonStatusTo.getBestillingsId());

        doknotifikasjonStatusValidator.validateInput(doknotifikasjonStatusTo);
        Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(doknotifikasjonStatusTo.getBestillingsId());

        if (notifikasjon == null) {
            log.warn("Notifikasjon med bestillingsId={} finnes ikke i notifikasjons databasen. Avslutter behandlingen. ",
                    doknotifikasjonStatusTo.getBestillingsId());
        } else {
            if (doknotifikasjonStatusTo.getDistribusjonId() != null) {
                handleEventWithDistribusjonId(notifikasjon, doknotifikasjonStatusTo);
            } else {
                handleEventWithoutDistribusjonId(notifikasjon, doknotifikasjonStatusTo);
            }
        }
    }

    private void handleEventWithDistribusjonId(Notifikasjon notifikasjon, DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
        if (isAllDistribusjonStatusEqualInputStatus(notifikasjon.getNotifikasjonDistribusjon(), doknotifikasjonStatusTo.getStatus())) {
            log.info("Alle distribusjoner knyttet til notifikasjon med bestillingsId={} har status={}. Ny hendelse skrives til kafka-topic {}.", doknotifikasjonStatusTo
                    .getBestillingsId(), doknotifikasjonStatusTo.getStatus(), KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS);
            publishNewDoknotifikasjonStatus(doknotifikasjonStatusTo);
        } else {
            log.info("Hendelsen på kafka-topic dok-eksternnotifikasjon-status har distribusjonsId. Avslutter behandlingen av hendelsen. ");
        }
    }

    private void handleEventWithoutDistribusjonId(Notifikasjon notifikasjon, DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
        if (Status.FERDIGSTILT.equals(doknotifikasjonStatusTo.getStatus()) && notifikasjon.getAntallRenotifikasjoner() > 0) {
            log.info("En hendelse på kafka-topic {} har status={} og notifikasjonen knyttet til " +
                    "hendelsen har mer enn null antall renotifikasjoner. Behandlingen av hendelsen avsluttes. ", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS, Status.FERDIGSTILT
                    .toString());
        } else {
            notifikasjon.setStatus(doknotifikasjonStatusTo.getStatus());
            notifikasjon.setEndretAv(doknotifikasjonStatusTo.getBestillerId());
            notifikasjon.setEndretDato(LocalDateTime.now());
        }
    }

    private void publishNewDoknotifikasjonStatus(DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
        kafkaDoknotifikasjonStatusProducer.publishDoknotifikasjonStatus(doknotifikasjonStatusTo
                        .getBestillingsId(), doknotifikasjonStatusTo.getBestillerId(),
                doknotifikasjonStatusTo.getStatus(), "notifikasjon er " + doknotifikasjonStatusTo
                        .getStatus(), null);
    }

    private boolean isAllDistribusjonStatusEqualInputStatus(Set<NotifikasjonDistribusjon> notifikasjonDistribusjonsSet, Status inputStatus) {
        if (notifikasjonDistribusjonsSet.isEmpty()) {
            return false;
        } else {
            for (NotifikasjonDistribusjon notifikasjonDistribusjon : notifikasjonDistribusjonsSet) {
                if (!inputStatus.equals(notifikasjonDistribusjon.getStatus())) {
                    return false;
                }
            }
            return true;
        }
    }
}
