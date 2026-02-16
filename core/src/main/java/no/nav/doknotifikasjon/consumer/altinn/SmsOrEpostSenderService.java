package no.nav.doknotifikasjon.consumer.altinn;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaTopics;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonService;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_SMS;
import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;

@Slf4j
public abstract class SmsOrEpostSenderService {

	protected final KafkaEventProducer kafkaEventProducer;
	protected final NotifikasjonDistribusjonService notifikasjonDistribusjonService;
	protected final AltinnVarselConsumer altinnVarselConsumer;

	protected SmsOrEpostSenderService(KafkaEventProducer kafkaEventProducer, NotifikasjonDistribusjonService notifikasjonDistribusjonService, AltinnVarselConsumer altinnVarselConsumer) {
		this.kafkaEventProducer = kafkaEventProducer;
		this.notifikasjonDistribusjonService = notifikasjonDistribusjonService;
		this.altinnVarselConsumer = altinnVarselConsumer;
	}

	protected void send(int notifikasjonDistribusjonId, Kanal kanal, String serviceName, String messageSuccessNotifikasjonStatus) {
		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonService.findById(notifikasjonDistribusjonId);
		Notifikasjon notifikasjon = notifikasjonDistribusjon.getNotifikasjon();
		final var bestillingsId = notifikasjon.getBestillingsId();

		DoknotifikasjonDistributableInChannel doknotifikasjonDistributableInChannel = mapNotifikasjonDistribusjon(notifikasjonDistribusjon);

		validateDistribusjonStatusOgKanal(doknotifikasjonDistributableInChannel, notifikasjon);
		try {
			log.info("{} kontakter Altinn for distribusjon av {}. notifikasjonDistribusjonId={}, bestillingsId={}", serviceName, kanal, notifikasjonDistribusjonId, bestillingsId);
			Optional<UUID> notificationOrderIdOptional = altinnVarselConsumer.sendVarsel(kanal, bestillingsId, doknotifikasjonDistributableInChannel.getKontaktInfo(), doknotifikasjonDistributableInChannel.getFodselsnummer(), doknotifikasjonDistributableInChannel.getTekst(), doknotifikasjonDistributableInChannel.getTittel());
			log.info("{} har sendt {} notifikasjon til Altinn OK.  notifikasjonDistribusjonId={}, bestillingsId={}", serviceName, kanal, notifikasjonDistribusjonId, bestillingsId);

			updateEntity(notifikasjonDistribusjon, notifikasjon.getBestillerId(), notificationOrderIdOptional);
			publishStatus(doknotifikasjonDistributableInChannel, Status.FERDIGSTILT, messageSuccessNotifikasjonStatus);
			registerMetricSent();
		} catch (AltinnFunctionalException altinnFunctionalException) {
			publishStatus(doknotifikasjonDistributableInChannel, FEILET, altinnFunctionalException.getMessage());
			throw altinnFunctionalException;
		} catch (Exception unknownException) {
			publishStatus(doknotifikasjonDistributableInChannel, FEILET, Optional.of(unknownException).map(Exception::getMessage).orElse(""));
			throw unknownException;
		}
	}

	protected abstract DoknotifikasjonDistributableInChannel mapNotifikasjonDistribusjon(NotifikasjonDistribusjon notifikasjonDistribusjon);
	protected abstract void validateDistribusjonStatusOgKanal(DoknotifikasjonDistributableInChannel doknotifikasjonDistributableInChannel, Notifikasjon notifikasjon);
	protected abstract void registerMetricSent();

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
