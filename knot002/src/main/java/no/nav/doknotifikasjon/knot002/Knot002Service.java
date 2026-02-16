package no.nav.doknotifikasjon.knot002;

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
import java.util.UUID;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SMS_UGYLDIG_KANAL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SMS_UGYLDIG_STATUS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_SMS;
import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;

@Slf4j
@Component
public class Knot002Service {

	private final Knot002Mapper knot002Mapper;
	private final MetricService metricService;
	private final KafkaEventProducer kafkaEventProducer;
	private final AltinnVarselConsumer altinnVarselConsumer;
	private final NotifikasjonDistribusjonService notifikasjonDistribusjonService;

	public Knot002Service(Knot002Mapper knot002Mapper, KafkaEventProducer kafkaEventProducer,
						  AltinnVarselConsumer altinnVarselConsumer, NotifikasjonDistribusjonService notifikasjonDistribusjonService,
						  MetricService metricService) {
		this.knot002Mapper = knot002Mapper;
		this.kafkaEventProducer = kafkaEventProducer;
		this.altinnVarselConsumer = altinnVarselConsumer;
		this.notifikasjonDistribusjonService = notifikasjonDistribusjonService;
		this.metricService = metricService;
	}

	public void shouldSendSms(int notifikasjonDistribusjonId) {
		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonService.findById(notifikasjonDistribusjonId);
		Notifikasjon notifikasjon = notifikasjonDistribusjon.getNotifikasjon();
		final var bestillingsId = notifikasjon.getBestillingsId();

		DoknotifikasjonSmsObject doknotifikasjonSmsObject = knot002Mapper.mapNotifikasjonDistrubisjon(notifikasjonDistribusjon, notifikasjon);

		if (!validateDistribusjonStatusOgKanal(doknotifikasjonSmsObject, notifikasjon)) {
			if (doknotifikasjonSmsObject.getDistribusjonStatus() == FERDIGSTILT) {
				return;
			}

			String melding = doknotifikasjonSmsObject.getDistribusjonStatus() == Status.OPPRETTET ? FEILET_SMS_UGYLDIG_KANAL : FEILET_SMS_UGYLDIG_STATUS;
			publishStatus(doknotifikasjonSmsObject, FEILET, melding);

			log.warn("Knot002 behandling av melding på kafka-topic={} avsluttes pga feil={}, bestillingsId={}", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_SMS, melding, bestillingsId);
			throw new DoknotifikasjonValidationException(String.format("Valideringsfeil oppstod i Knot002. Feilmelding: %s", melding));
		}

		try {
			log.info("Knot002 kontakter Altinn for distribusjon av SMS. notifikasjonDistribusjonId={}, bestillingsId={}", notifikasjonDistribusjonId, bestillingsId);
			Optional<UUID> notificationOrderIdOptional = altinnVarselConsumer.sendSmsVarsel(bestillingsId, doknotifikasjonSmsObject.getKontaktInfo(), doknotifikasjonSmsObject.getFodselsnummer(), doknotifikasjonSmsObject.getTekst());
			log.info("Knot002 har sendt SMS notifikasjon til Altinn OK.  notifikasjonDistribusjonId={}, bestillingsId={}", notifikasjonDistribusjonId, bestillingsId);

			updateEntity(notifikasjonDistribusjon, notifikasjon.getBestillerId(), notificationOrderIdOptional);
			publishStatus(doknotifikasjonSmsObject, Status.FERDIGSTILT, FERDIGSTILT_NOTIFIKASJON_SMS);
			metricService.metricKnot002SmsSent();
		} catch (AltinnFunctionalException altinnFunctionalException) {
			publishStatus(doknotifikasjonSmsObject, FEILET, altinnFunctionalException.getMessage());
			throw altinnFunctionalException;
		} catch (Exception unknownException) {
			publishStatus(doknotifikasjonSmsObject, FEILET, Optional.of(unknownException).map(Exception::getMessage).orElse(""));
			throw unknownException;
		}
	}

	private boolean validateDistribusjonStatusOgKanal(DoknotifikasjonSmsObject doknotifikasjonSmsObject, Notifikasjon notifikasjon) {
		if (notifikasjon.getStatus() == FERDIGSTILT) {
			throw new NotifikasjonFerdigstiltFunctionalException("Notifikasjonen har status ferdigstilt, vil avslutte utsendelsen av epost for knot003.");
		}

		return Status.OPPRETTET.equals(doknotifikasjonSmsObject.getDistribusjonStatus())
				&& Kanal.SMS.equals(doknotifikasjonSmsObject.getKanal());
	}

	private void publishStatus(DoknotifikasjonSmsObject doknotifikasjonSmsObject, Status status, String melding) {
		kafkaEventProducer.publish(
				KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS,
				DoknotifikasjonStatus.newBuilder()
						.setBestillerId(doknotifikasjonSmsObject.getBestillerId())
						.setBestillingsId(doknotifikasjonSmsObject.getBestillingsId())
						.setStatus(status.name())
						.setMelding(melding)
						.setDistribusjonId(doknotifikasjonSmsObject.getNotifikasjonDistribusjonId())
						.build()
		);
	}

	public void updateEntity(NotifikasjonDistribusjon notifikasjonDistribusjon, String bestillerId, Optional<UUID> altinnNotificationOrderId) {
		LocalDateTime now = LocalDateTime.now();
		notifikasjonDistribusjon.setEndretAv(bestillerId);
		notifikasjonDistribusjon.setStatus(Status.FERDIGSTILT);
		notifikasjonDistribusjon.setSendtDato(now);
		notifikasjonDistribusjon.setEndretDato(now);
		altinnNotificationOrderId.ifPresent(notifikasjonDistribusjon::setAltinnNotificationOrderId);

		notifikasjonDistribusjonService.save(notifikasjonDistribusjon);
	}
}
