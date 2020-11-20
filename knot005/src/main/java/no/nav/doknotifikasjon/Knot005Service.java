package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDateTime;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_RENOTIFIKASJON_STANSET;

@Slf4j
@Component
public class Knot005Service {

	private final NotifikasjonService notifikasjonService;
	private final KafkaStatusEventProducer kafkaDoknotifikasjonStatusProducer;
	private final DoknotifikasjonStoppValidadator doknotifikasjonStoppValidadator;
	private final MetricService metricService;

	@Inject
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
		Notifikasjon notifikasjon = notifikasjonService.findByBestillingsId(doknotifikasjonStoppTo.getBestillingsId());

		if (notifikasjon == null) {
			log.warn("Notifikasjon med bestillingsId={} finnes ikke i notifikasjons databasen. Avslutter behandlingen. ",
					doknotifikasjonStoppTo.getBestillingsId());
		} else if (Status.FERDIGSTILT.equals(notifikasjon.getStatus())) {
			log.warn("Notifikasjon med bestillingsId={} har status={}. Avslutter behandlingen. ",
					doknotifikasjonStoppTo.getBestillingsId(), Status.FERDIGSTILT);
		} else {
			log.info("Knot005 oppdaterer notifikasjon med bestillingsId={}", doknotifikasjonStoppTo.getBestillingsId());
			updateNotifikasjon(notifikasjon, doknotifikasjonStoppTo);
			publishNewDoknotifikasjonStatus(doknotifikasjonStoppTo);
			metricService.metricKnot005ReNotifikasjonStopped();
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

		notifikasjonService.save(notifikasjon);
	}
}
