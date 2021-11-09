package no.nav.doknotifikasjon.knot002;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.consumer.altinn.AltinnVarselConsumer;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaTopics;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistrubisjonService;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Optional;

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
	private final NotifikasjonDistrubisjonService notifikasjonDistrubisjonService;

	@Inject
	public Knot002Service(Knot002Mapper knot002Mapper, KafkaEventProducer kafkaEventProducer,
						  AltinnVarselConsumer altinnVarselConsumer, NotifikasjonDistrubisjonService notifikasjonDistrubisjonService,
						  MetricService metricService) {
		this.knot002Mapper = knot002Mapper;
		this.kafkaEventProducer = kafkaEventProducer;
		this.altinnVarselConsumer = altinnVarselConsumer;
		this.notifikasjonDistrubisjonService = notifikasjonDistrubisjonService;
		this.metricService = metricService;
	}

	public void shouldSendSms(int notifikasjonDistribusjonId) {
		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistrubisjonService.findById(notifikasjonDistribusjonId);
		Notifikasjon notifikasjon = notifikasjonDistribusjon.getNotifikasjon();

		DoknotifikasjonSmsObject doknotifikasjonSmsObject = knot002Mapper.mapNotifikasjonDistrubisjon(notifikasjonDistribusjon, notifikasjon);

		if (!validateDistribusjonStatusOgKanal(doknotifikasjonSmsObject, notifikasjon)) {
			String melding = doknotifikasjonSmsObject.getDistribusjonStatus() == Status.OPPRETTET ? FEILET_SMS_UGYLDIG_KANAL : FEILET_SMS_UGYLDIG_STATUS;
			publishStatus(doknotifikasjonSmsObject, FEILET, melding);

			log.warn("Behandling av melding på kafka-topic={} avsluttes pga feil={}", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS, melding);
			throw new DoknotifikasjonValidationException(String.format("Valideringsfeil oppstod i Knot002. Feilmelding: %s", melding));
		}

		try {
			log.info("Knot002 kontakter Altinn for distribusjon av notifikasjonDistribusjon med id={}", notifikasjonDistribusjonId);
			altinnVarselConsumer.sendVarsel(Kanal.SMS, doknotifikasjonSmsObject.getKontaktInfo(), doknotifikasjonSmsObject.getFodselsnummer(), doknotifikasjonSmsObject.getTekst(), "");
			log.info(FERDIGSTILT_NOTIFIKASJON_SMS + " for notifikasjonDistribusjon med Id={}", notifikasjonDistribusjonId);
		} catch (AltinnFunctionalException altinnFunctionalException) {
			publishStatus(doknotifikasjonSmsObject, FEILET, altinnFunctionalException.getMessage());
			throw altinnFunctionalException;
		} catch (Exception unknownException) {
			publishStatus(doknotifikasjonSmsObject, FEILET, Optional.of(unknownException).map(Exception::getMessage).orElse(""));
			throw unknownException;
		}

		updateEntity(notifikasjonDistribusjon, notifikasjon.getBestillerId());
		publishStatus(doknotifikasjonSmsObject, Status.FERDIGSTILT, FERDIGSTILT_NOTIFIKASJON_SMS);
		metricService.metricKnot002SmsSent();
	}

	private boolean validateDistribusjonStatusOgKanal(DoknotifikasjonSmsObject doknotifikasjonSmsObject, Notifikasjon notifikasjon) {
		return Status.OPPRETTET.equals(doknotifikasjonSmsObject.getDistribusjonStatus())
				&& Kanal.SMS.equals(doknotifikasjonSmsObject.getKanal())
				&& notifikasjon.getStatus() != FERDIGSTILT;
	}

	private void publishStatus(DoknotifikasjonSmsObject doknotifikasjonSmsObject, Status status, String melding) {
		kafkaEventProducer.publish(
				KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
				DoknotifikasjonStatus.newBuilder()
						.setBestillerId(doknotifikasjonSmsObject.getBestillerId())
						.setBestillingsId(doknotifikasjonSmsObject.getBestillingsId())
						.setStatus(status.name())
						.setMelding(melding)
						.setDistribusjonId(doknotifikasjonSmsObject.getNotifikasjonDistribusjonId())
						.build()
		);
	}

	public void updateEntity(NotifikasjonDistribusjon notifikasjonDistribusjon, String bestillerId) {
		LocalDateTime now = LocalDateTime.now();
		notifikasjonDistribusjon.setEndretAv(bestillerId);
		notifikasjonDistribusjon.setStatus(Status.FERDIGSTILT);
		notifikasjonDistribusjon.setSendtDato(now);
		notifikasjonDistribusjon.setEndretDato(now);

		notifikasjonDistrubisjonService.save(notifikasjonDistribusjon);
	}
}