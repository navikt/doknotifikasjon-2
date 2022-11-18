package no.nav.doknotifikasjon.knot003;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.consumer.altinn.AltinnVarselConsumer;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.exception.functional.NotifikasjonFerdigstiltFunctionalException;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaTopics;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonService;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_EPOST_UGYLDIG_KANAL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_EPOST_UGYLDIG_STATUS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_EPOST;
import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;

@Slf4j
@Component
public class Knot003Service {

	private final Knot003Mapper knot003Mapper;
	private final KafkaEventProducer kafkaEventProducer;
	private final AltinnVarselConsumer altinnVarselConsumer;
	private final MetricService metricService;
	private final NotifikasjonDistribusjonService notifikasjonDistribusjonService;

	public Knot003Service(Knot003Mapper knot003Mapper, KafkaEventProducer kafkaEventProducer,
						  AltinnVarselConsumer altinnVarselConsumer, MetricService metricService,
						  NotifikasjonDistribusjonService notifikasjonDistribusjonService) {
		this.kafkaEventProducer = kafkaEventProducer;
		this.altinnVarselConsumer = altinnVarselConsumer;
		this.metricService = metricService;
		this.knot003Mapper = knot003Mapper;
		this.notifikasjonDistribusjonService = notifikasjonDistribusjonService;
	}

	public void shouldSendEpost(int notifikasjonDistribusjonId) {
		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonService.findById(notifikasjonDistribusjonId);
		Notifikasjon notifikasjon = notifikasjonDistribusjon.getNotifikasjon();
		final var bestillingsId = notifikasjon.getBestillingsId();

		DoknotifikasjonEpostObject doknotifikasjonEpostObject = knot003Mapper.mapNotifikasjonDistrubisjon(notifikasjonDistribusjon, notifikasjon);

		if (!validateDistribusjonStatusOgKanal(doknotifikasjonEpostObject, notifikasjon)) {
			String melding = doknotifikasjonEpostObject.getDistribusjonStatus() == OPPRETTET ? FEILET_EPOST_UGYLDIG_KANAL : FEILET_EPOST_UGYLDIG_STATUS;
			publishStatus(doknotifikasjonEpostObject, FEILET, melding);

			log.warn("Knot003 behandling av melding på kafka-topic={} avsluttes pga feil={}, bestillingsId={}", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_EPOST, melding, bestillingsId);
			throw new DoknotifikasjonValidationException(String.format("Valideringsfeil oppstod i Knot003. Feilmelding: %s", melding));
		}

		try {
			log.info("Knot003 kontakter Altinn for distribusjon av EPOST. notifikasjonDistribusjonId={}, bestillingsId={}", notifikasjonDistribusjonId, bestillingsId);
			altinnVarselConsumer.sendVarsel(Kanal.EPOST, doknotifikasjonEpostObject.getKontaktInfo(), doknotifikasjonEpostObject.getFodselsnummer(), doknotifikasjonEpostObject.getTekst(), doknotifikasjonEpostObject.getTittel());
			log.info("Knot003 har sendt EPOST notifikasjon til Altinn OK.  notifikasjonDistribusjonId={}, bestillingsId={}", notifikasjonDistribusjonId, bestillingsId);
		} catch (AltinnFunctionalException altinnFunctionalException) {
			publishStatus(doknotifikasjonEpostObject, FEILET, altinnFunctionalException.getMessage());
			throw altinnFunctionalException;
		} catch (Exception unknownException) {
			publishStatus(doknotifikasjonEpostObject, FEILET, Optional.of(unknownException).map(Exception::getMessage).orElse(""));
			throw unknownException;
		}

		updateEntity(notifikasjonDistribusjon, notifikasjon.getBestillerId());

		publishStatus(doknotifikasjonEpostObject, Status.FERDIGSTILT, FERDIGSTILT_NOTIFIKASJON_EPOST);
		metricService.metricKnot003EpostSent();
	}

	private boolean validateDistribusjonStatusOgKanal(DoknotifikasjonEpostObject doknotifikasjonEpostObject, Notifikasjon notifikasjon) {
		if (notifikasjon.getStatus() == FERDIGSTILT) {
			throw new NotifikasjonFerdigstiltFunctionalException("Notifikasjonen har status ferdigstilt, vil avslutte utsendelsen av epost for knot003.");
		}

		return OPPRETTET.equals(doknotifikasjonEpostObject.getDistribusjonStatus())
				&& Kanal.EPOST.equals(doknotifikasjonEpostObject.getKanal());
	}

	private void publishStatus(DoknotifikasjonEpostObject doknotifikasjonEpostObject, Status status, String melding) {
		kafkaEventProducer.publish(
				KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS,
				DoknotifikasjonStatus.newBuilder()
						.setBestillerId(doknotifikasjonEpostObject.getBestillerId())
						.setBestillingsId(doknotifikasjonEpostObject.getBestillingsId())
						.setStatus(status.name())
						.setMelding(melding)
						.setDistribusjonId(doknotifikasjonEpostObject.getNotifikasjonDistribusjonId())
						.build()
		);
	}

	public void updateEntity(NotifikasjonDistribusjon notifikasjonDistribusjon, String bestillerId) {
		LocalDateTime now = LocalDateTime.now();
		notifikasjonDistribusjon.setEndretAv(bestillerId);
		notifikasjonDistribusjon.setStatus(Status.FERDIGSTILT);
		notifikasjonDistribusjon.setSendtDato(now);
		notifikasjonDistribusjon.setEndretDato(now);

		notifikasjonDistribusjonService.save(notifikasjonDistribusjon);
	}
}