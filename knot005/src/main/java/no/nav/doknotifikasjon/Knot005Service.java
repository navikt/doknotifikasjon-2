package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_RENOTIFIKASJON_STANSET;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;

@Slf4j
@Component
public class Knot005Service {

	private final NotifikasjonService notifikasjonService;
	private final KafkaStatusEventProducer kafkaDoknotifikasjonStatusProducer;
	private final DoknotifikasjonStoppValidadator doknotifikasjonStoppValidadator;
	private final MetricService metricService;

	@Autowired
	public Knot005Service(NotifikasjonService notifikasjonService,
						  KafkaStatusEventProducer kafkaDoknotifikasjonStatusProducer,
						  DoknotifikasjonStoppValidadator doknotifikasjonStoppValidadator,
						  MetricService metricService) {
		this.notifikasjonService = notifikasjonService;
		this.kafkaDoknotifikasjonStatusProducer = kafkaDoknotifikasjonStatusProducer;
		this.doknotifikasjonStoppValidadator = doknotifikasjonStoppValidadator;
		this.metricService = metricService;
	}

	public void shouldStopResending(DoknotifikasjonStoppTo doknotifikasjonStoppTo) {
		doknotifikasjonStoppValidadator.validateInput(doknotifikasjonStoppTo);
		Notifikasjon notifikasjon = notifikasjonService.findByBestillingsIdIngenRetryForNotifikasjonIkkeFunnet(doknotifikasjonStoppTo.getBestillingsId());

		if (notifikasjon == null) {
			log.warn("Knot005 Notifikasjon med bestillingsId={} finnes ikke i databasen. Avslutter behandlingen.",
					doknotifikasjonStoppTo.getBestillingsId());
		} else if (FERDIGSTILT.equals(notifikasjon.getStatus())) {
			log.info("Knot005 Notifikasjon med bestillingsId={} har status={}. Avslutter behandlingen.",
					doknotifikasjonStoppTo.getBestillingsId(), FERDIGSTILT);
		} else {
			log.info("Knot005 oppdaterer notifikasjon med bestillingsId={}", doknotifikasjonStoppTo.getBestillingsId());
			updateNotifikasjon(notifikasjon, doknotifikasjonStoppTo);
			publishNewDoknotifikasjonStatus(doknotifikasjonStoppTo);
			metricService.metricKnot005ReNotifikasjonStopped();
		}
	}

	private void publishNewDoknotifikasjonStatus(DoknotifikasjonStoppTo doknotifikasjonStoppTo) {
		kafkaDoknotifikasjonStatusProducer.publishDoknotifikasjonStatusFerdigstilt(
				doknotifikasjonStoppTo.getBestillingsId(),
				doknotifikasjonStoppTo.getBestillerId(),
				FERDIGSTILT_RENOTIFIKASJON_STANSET,
				null
		);
	}

	private void updateNotifikasjon(Notifikasjon notifikasjon, DoknotifikasjonStoppTo doknotifikasjonStoppTo) {
		notifikasjon.setAntallRenotifikasjoner(0);
		notifikasjon.setNesteRenotifikasjonDato(null);
		notifikasjon.setEndretAv(doknotifikasjonStoppTo.getBestillerId());
		notifikasjon.setEndretDato(LocalDateTime.now());
		notifikasjon.setStatus(FERDIGSTILT);
		
		notifikasjonService.save(notifikasjon);
	}
}
