package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaDoknotifikasjonStatusProducer;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_RENOTIFIKASJON_STANSET;

@Slf4j
@Component
public class Knot005Service {

    private final NotifikasjonRepository notifikasjonRepository;
    private final KafkaDoknotifikasjonStatusProducer kafkaDoknotifikasjonStatusProducer;
    private final DoknotifikasjonStoppValidadator doknotifikasjonStoppValidadator;

    public Knot005Service(NotifikasjonRepository notifikasjonRepository,
                          KafkaDoknotifikasjonStatusProducer kafkaDoknotifikasjonStatusProducer,
                          DoknotifikasjonStoppValidadator doknotifikasjonStoppValidadator) {
        this.notifikasjonRepository = notifikasjonRepository;
        this.kafkaDoknotifikasjonStatusProducer = kafkaDoknotifikasjonStatusProducer;
        this.doknotifikasjonStoppValidadator = doknotifikasjonStoppValidadator;
    }

    public void shouldStopResending(DoknotifikasjonStoppTo doknotifikasjonStoppTo) {
        doknotifikasjonStoppValidadator.validateInput(doknotifikasjonStoppTo);

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
                doknotifikasjonStoppTo.getBestillerId(), FERDIGSTILT_RENOTIFIKASJON_STANSET, null);
    }

    private void updateNotifikasjon(Notifikasjon notifikasjon, DoknotifikasjonStoppTo doknotifikasjonStoppTo) {
        notifikasjon.setAntallRenotifikasjoner(0);
        notifikasjon.setNesteRenotifikasjonDato(null);
        notifikasjon.setEndretAv(doknotifikasjonStoppTo.getBestillerId());
        notifikasjon.setEndretDato(LocalDateTime.now());
    }
}
