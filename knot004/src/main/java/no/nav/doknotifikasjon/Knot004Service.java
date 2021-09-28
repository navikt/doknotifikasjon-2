package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Set;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS;
import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static no.nav.doknotifikasjon.kodeverk.Status.INFO;
import static no.nav.doknotifikasjon.kodeverk.Status.OVERSENDT;

@Slf4j
@Component
public class Knot004Service {

	private final NotifikasjonService notifikasjonService;
	private final DoknotifikasjonStatusValidator doknotifikasjonStatusValidator;
	private final KafkaStatusEventProducer kafkaDoknotifikasjonStatusProducer;
	private final MetricService metricService;

	@Inject
	public Knot004Service(
			NotifikasjonService notifikasjonService,
			DoknotifikasjonStatusValidator doknotifikasjonStatusValidator,
			KafkaStatusEventProducer kafkaDoknotifikasjonStatusProducer,
			MetricService metricService
	) {
		this.notifikasjonService = notifikasjonService;
		this.doknotifikasjonStatusValidator = doknotifikasjonStatusValidator;
		this.kafkaDoknotifikasjonStatusProducer = kafkaDoknotifikasjonStatusProducer;
		this.metricService = metricService;
	}

	public void shouldUpdateStatus(DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
		metricService.metricKnot004Status(doknotifikasjonStatusTo.getStatus());

		if (INFO.equals(doknotifikasjonStatusTo.getStatus())) {
			log.info("Melding med status {} skal ikke oppdatere status. Avslutter behandlingen.", INFO);
			return;
		}

		doknotifikasjonStatusValidator.validateInput(doknotifikasjonStatusTo);
		Notifikasjon notifikasjon = notifikasjonService.findByBestillingsId(doknotifikasjonStatusTo.getBestillingsId());

		if (notifikasjon == null) {
			log.warn("Notifikasjon med bestillingsId={} finnes ikke i notifikasjonsdatabasen. Avslutter behandlingen.",
					doknotifikasjonStatusTo.getBestillingsId());
			return;
		}

		if (doknotifikasjonStatusTo.getDistribusjonId() != null) {
			handleEventWithDistribusjonId(notifikasjon, doknotifikasjonStatusTo);
		} else if (statusIsNewerThanPreviousStatus(doknotifikasjonStatusTo, notifikasjon)) {
			handleEventWithoutDistribusjonId(notifikasjon, doknotifikasjonStatusTo);
		}
	}

	boolean statusIsNewerThanPreviousStatus(DoknotifikasjonStatusTo doknotifikasjonStatusTo, Notifikasjon notifikasjon) {
		Status newStatus = doknotifikasjonStatusTo.getStatus();
		Status oldStatus = notifikasjon.getStatus();

		return newStatus.getPriority() > oldStatus.getPriority();
	}

	private void handleEventWithDistribusjonId(Notifikasjon notifikasjon, DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
		if (isAllDistribusjonStatusEqualInputStatus(notifikasjon.getNotifikasjonDistribusjon(), doknotifikasjonStatusTo.getStatus())) {
			log.info("Alle distribusjoner knyttet til notifikasjon med bestillingsId={} har status={}. Ny hendelse skrives til kafka-topic={}.",
					doknotifikasjonStatusTo
					.getBestillingsId(),
					doknotifikasjonStatusTo.getStatus(),
					KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS
			);
			publishNewDoknotifikasjonStatus(doknotifikasjonStatusTo);
		} else {
			log.info("Hendelsen på kafka-topic dok-eksternnotifikasjon-status har distribusjonsId. Avslutter behandlingen av hendelsen. ");
		}
	}

	private void handleEventWithoutDistribusjonId(Notifikasjon notifikasjon, DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
		if (FERDIGSTILT.equals(doknotifikasjonStatusTo.getStatus()) && notifikasjon.getAntallRenotifikasjoner() != null && notifikasjon.getAntallRenotifikasjoner() > 0) {
			log.info("En hendelse på kafka-topic {} har status={} og notifikasjonen knyttet til hendelsen har mer enn null antall renotifikasjoner. Behandlingen av hendelsen avsluttes. ",
					KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
					FERDIGSTILT.toString()
			);
		} else {
			notifikasjon.setStatus(doknotifikasjonStatusTo.getStatus());
			notifikasjon.setEndretAv(doknotifikasjonStatusTo.getBestillerId());
			notifikasjon.setEndretDato(LocalDateTime.now());

			log.info("Status på notifikasjon med bestillingsId={} har blitt oppdatert til status={}", notifikasjon.getBestillingsId(), notifikasjon.getStatus());
			notifikasjonService.save(notifikasjon);
		}
	}

	private void publishNewDoknotifikasjonStatus(DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
		kafkaDoknotifikasjonStatusProducer.publishDoknotifikasjonStatus(
				doknotifikasjonStatusTo.getBestillingsId(),
				doknotifikasjonStatusTo.getBestillerId(),
				doknotifikasjonStatusTo.getStatus(),
				"notifikasjon er " + doknotifikasjonStatusTo.getStatus(),
				null
		);
	}

	private boolean isAllDistribusjonStatusEqualInputStatus(Set<NotifikasjonDistribusjon> notifikasjonDistribusjonsSet, Status inputStatus) {
		if (FEILET.equals(inputStatus)) {
			return true;
		} else if (notifikasjonDistribusjonsSet.isEmpty()) {
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
