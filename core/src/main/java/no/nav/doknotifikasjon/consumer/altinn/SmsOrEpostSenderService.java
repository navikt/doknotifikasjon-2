package no.nav.doknotifikasjon.consumer.altinn;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.AbstractDoknotifikasjonFunctionalException;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static no.nav.doknotifikasjon.metrics.MetricTags.FUNCTIONAL;

@Slf4j
public abstract class SmsOrEpostSenderService<T extends DoknotifikasjonDistributableInChannel> {

	private final KafkaEventProducer kafkaEventProducer;
	private final NotifikasjonDistribusjonService notifikasjonDistribusjonService;
	private final MetricService metricService;

	protected SmsOrEpostSenderService(KafkaEventProducer kafkaEventProducer, NotifikasjonDistribusjonService notifikasjonDistribusjonService, MetricService metricService) {
		this.kafkaEventProducer = kafkaEventProducer;
		this.notifikasjonDistribusjonService = notifikasjonDistribusjonService;
		this.metricService = metricService;
	}

	protected abstract Optional<UUID> sendVarselToKanal(String bestillingsId, T doknotifikasjonDistributableInChannel);
	protected abstract T mapNotifikasjonDistribusjon(NotifikasjonDistribusjon notifikasjonDistribusjon);

	protected abstract String messageSuccessNotifikasjonStatus();
	protected abstract String messageInvalidDistribusjonStatusMessage();
	protected abstract String messageInvalidChannelMessage();

	protected void send(int notifikasjonDistribusjonId, Kanal kanal, String serviceName) {
		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonService.findById(notifikasjonDistribusjonId);
		Notifikasjon notifikasjon = notifikasjonDistribusjon.getNotifikasjon();
		String bestillingsId = notifikasjon.getBestillingsId();

		T doknotifikasjonDistributableInChannel = mapNotifikasjonDistribusjon(notifikasjonDistribusjon);

		if (notifikasjon.getStatus() == FERDIGSTILT) {
			String reason = String.format("Notifikasjonen har status ferdigstilt, vil avslutte utsendelsen av %s for %s.", kanal, serviceName);
			log.warn(reason);
			permanentlyFailMessageWithReason(doknotifikasjonDistributableInChannel, reason, NotifikasjonFerdigstiltFunctionalException.class.getSimpleName());
			return;
		}
		if (Status.OPPRETTET != doknotifikasjonDistributableInChannel.getDistribusjonStatus()) {
			permanentlyFailMessageWithReason(doknotifikasjonDistributableInChannel, messageInvalidDistribusjonStatusMessage(), DoknotifikasjonValidationException.class.getSimpleName());
			log.error("{} behandling av melding på kafka-topic={} avsluttes pga feil={}, bestillingsId={}", serviceName, KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_SMS, messageInvalidDistribusjonStatusMessage(), bestillingsId);
			return;
		}
		if (kanal != doknotifikasjonDistributableInChannel.getKanal()) {
			permanentlyFailMessageWithReason(doknotifikasjonDistributableInChannel, messageInvalidChannelMessage(), DoknotifikasjonValidationException.class.getSimpleName());
			log.error("{} behandling av melding på kafka-topic={} avsluttes pga feil={}, bestillingsId={}", serviceName, KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_SMS, messageInvalidChannelMessage(), bestillingsId);
			return;
		}

		try {
			log.info("{} kontakter Altinn for distribusjon av {}. notifikasjonDistribusjonId={}, bestillingsId={}", serviceName, kanal, notifikasjonDistribusjonId, bestillingsId);
			Optional<UUID> notificationOrderIdOptional = sendVarselToKanal(bestillingsId, doknotifikasjonDistributableInChannel);
			log.info("{} har sendt {} notifikasjon til Altinn OK.  notifikasjonDistribusjonId={}, bestillingsId={}, notifikasjonOrdreId={}", serviceName, kanal, notifikasjonDistribusjonId, bestillingsId, notificationOrderIdOptional.map(UUID::toString).orElse("\"\""));

			updateEntity(notifikasjonDistribusjon, notifikasjon.getBestillerId(), notificationOrderIdOptional);
			publishStatus(doknotifikasjonDistributableInChannel, Status.FERDIGSTILT, messageSuccessNotifikasjonStatus());
		} catch (AltinnFunctionalException e) {
			log.warn("{} NotifikasjonDistribusjonConsumer funksjonell feil ved kall mot Altinn. ", serviceName, e);
			permanentlyFailMessageWithException(e, doknotifikasjonDistributableInChannel);
		} catch (DoknotifikasjonDistribusjonIkkeFunnetException e) {
			log.error("Ingen NotifikasjonDistribusjon med notifikasjonDistribusjonId={} ble funnet i databasen for {} ({}). Dette må følges opp.", notifikasjonDistribusjonId, serviceName, kanal, e);
			permanentlyFailMessageWithException(e, doknotifikasjonDistributableInChannel);
		}
	}

	private void permanentlyFailMessageWithException(AbstractDoknotifikasjonFunctionalException e, T doknotifikasjonDistributableInChannel) {
		publishStatus(doknotifikasjonDistributableInChannel, FEILET, e.getMessage());
		metricService.metricHandleException(e);
	}

	private void permanentlyFailMessageWithReason(T doknotifikasjonDistributableInChannel, String reason, String metricErrorName) {
		publishStatus(doknotifikasjonDistributableInChannel, FEILET, reason);
		metricService.metricHandleError(FUNCTIONAL, metricErrorName);
	}

	protected void publishStatus(DoknotifikasjonDistributableInChannel doknotifikasjonObject, Status status, String melding) {
		kafkaEventProducer.publish(
			KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS,
			DoknotifikasjonStatus.newBuilder()
				.setBestillerId(doknotifikasjonObject.getBestillerId())
				.setBestillingsId(doknotifikasjonObject.getBestillingsId())
				.setStatus(status.name())
				.setMelding(melding)
				.setDistribusjonId(doknotifikasjonObject.getNotifikasjonDistribusjonId())
				.build()
		);
	}

	private void updateEntity(NotifikasjonDistribusjon notifikasjonDistribusjon, String bestillerId, Optional<UUID> altinnNotificationOrderId) {
		LocalDateTime now = LocalDateTime.now();
		notifikasjonDistribusjon.setEndretAv(bestillerId);
		notifikasjonDistribusjon.setStatus(Status.FERDIGSTILT);
		notifikasjonDistribusjon.setSendtDato(now);
		notifikasjonDistribusjon.setEndretDato(now);
		altinnNotificationOrderId.ifPresent(notifikasjonDistribusjon::setAltinnNotificationOrderId);

		notifikasjonDistribusjonService.save(notifikasjonDistribusjon);
	}
}
