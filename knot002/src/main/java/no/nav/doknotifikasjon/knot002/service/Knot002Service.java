package no.nav.doknotifikasjon.knot002.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.consumer.altinn.AltinnConsumer;
import no.nav.doknotifikasjon.knot002.domain.DoknotifikasjonSms;
import no.nav.doknotifikasjon.knot002.mapper.Knot002NotifikasjonEntityMapper;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.doknotifikasjon.kafka.KafkaTopics;
import org.springframework.stereotype.Component;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.util.Optional;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_SMS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SMS_UGYLDIG_STATUS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SMS_UGYLDIG_KANAL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_DATABASE_IKKE_OPPDATERT;

@Slf4j
@Component
public class Knot002Service {

	private final Knot002NotifikasjonEntityMapper notifikasjonEntityMapper;
	private final KafkaEventProducer kafkaEventProducer;
	private final AltinnConsumer altinnConsumer;
	private final MetricService metricService;

	public Knot002Service(
			Knot002NotifikasjonEntityMapper notifikasjonEntityMapper,
			KafkaEventProducer kafkaEventProducer,
			AltinnConsumer altinnConsumer,
			MetricService metricService
	) {
		this.notifikasjonEntityMapper = notifikasjonEntityMapper;
		this.kafkaEventProducer = kafkaEventProducer;
		this.altinnConsumer = altinnConsumer;
		this.metricService = metricService;
	}

	public void konsumerDistribusjonId(int notifikasjonDistribusjonId) {

		DoknotifikasjonSms doknotifikasjonSms;

		try {
			doknotifikasjonSms = notifikasjonEntityMapper.mapNotifikasjonDistrubisjon(notifikasjonDistribusjonId);
		} catch (Exception exception) {
			log.error("NotifikasjonDistribusjonConsumer kunne ikke hente notifikasjon", exception);
			return;
		}

		if (!validateDistribusjonStatusOgKanal(doknotifikasjonSms)) {
			String melding = doknotifikasjonSms.distribusjonStatus == Status.OPPRETTET
					? FEILET_SMS_UGYLDIG_KANAL
					: FEILET_SMS_UGYLDIG_STATUS;

			publishStatus(doknotifikasjonSms, Status.FEILET, melding);
			return;
		}

		try {
			altinnConsumer.sendStandaloneNotificationV3(
					Kanal.SMS,
					doknotifikasjonSms.kontakt,
					doknotifikasjonSms.tekst
			);

			log.info(FERDIGSTILT_NOTIFIKASJON_SMS + " notifikasjonDistribusjonId={}", notifikasjonDistribusjonId);
		} catch (SoapFaultClientException soapFault) {
			log.error("Knot002Service har mottatt faultmelding fra altinn fault reason={}", soapFault.getFaultStringOrReason(), soapFault);
			publishStatus(doknotifikasjonSms, Status.FEILET, soapFault.getFaultStringOrReason());
			return;

		} catch (AltinnFunctionalException altinnFunctionalException) {
			log.error("Knot002Service  funksjonell feil ved kall mot altinn: feilmelding={}", altinnFunctionalException.getMessage(), altinnFunctionalException);
			publishStatus(doknotifikasjonSms, Status.FEILET, altinnFunctionalException.getMessage());
			return;
		} catch (Exception unknownException) {
			log.error("Knot002Service ukjent exception", unknownException);
			publishStatus(
					doknotifikasjonSms,
					Status.FEILET,
					Optional.of(unknownException).map(Exception::getMessage).orElse("Knot002Service ukjent exception")
			);
			return;
		}


		try {
			notifikasjonEntityMapper.updateEntity(
					notifikasjonDistribusjonId,
					doknotifikasjonSms.bestillerId
			);
		} catch (Exception exception) {
			log.error(FEILET_DATABASE_IKKE_OPPDATERT, exception);
			// Uendelig retry ved databasefeil
			// Alexander-Haugli:
			// "og så har vi tenkt "uendelig" retry med overvåkning på grafana dashboard.
			// ikke helt overbevist om at det er optimalt, men hvis altinn eller db gir tekniske feil,
			// så har det ikke så mye for seg å gå videre (ingen grunn til å tro at det går bedre med neste melding)
			// - så at vi får en "propp" i behandlingen er kanskje ikke så feil"
			return;
		}

		publishStatus(doknotifikasjonSms, Status.FERDIGSTILT, FERDIGSTILT_NOTIFIKASJON_SMS);
		metricService.metricKnot002SmsProcessed();
	}

	private boolean validateDistribusjonStatusOgKanal(DoknotifikasjonSms doknotifikasjonSms) {
		return doknotifikasjonSms.getDistribusjonStatus().equals(Status.OPPRETTET)
				&& doknotifikasjonSms.kanal.equals(Kanal.SMS);
	}


	private void publishStatus(DoknotifikasjonSms doknotifikasjonSms, Status status, String melding) {
		kafkaEventProducer.publish(
				KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
				doknotifikasjonSms.notifikasjonDistribusjonId,
				DoknotifikasjonStatus.newBuilder()
						.setBestillerId(doknotifikasjonSms.bestillerId)
						.setBestillingsId(doknotifikasjonSms.bestillingsId)
						.setStatus(status.name())
						.setMelding(melding)
						.setDistribusjonId(Long.valueOf(doknotifikasjonSms.getNotifikasjonDistribusjonId()))
						.build(),
				System.currentTimeMillis()
		);
	}

}
