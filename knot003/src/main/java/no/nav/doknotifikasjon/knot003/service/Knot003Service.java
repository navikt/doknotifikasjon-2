package no.nav.doknotifikasjon.knot003.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.consumer.altinn.AltinnConsumer;
import no.nav.doknotifikasjon.knot003.domain.DoknotifikasjonEpost;
import no.nav.doknotifikasjon.knot003.mapper.Knoot003NotifikasjonEntityMapper;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.doknotifikasjon.kafka.KafkaTopics;
import org.springframework.stereotype.Component;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.util.Optional;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_EPOST;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_EPOST_UGYLDIG_STATUS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_EPOST_UGYLDIG_KANAL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_DATABASE_IKKE_OPPDATERT;

@Slf4j
@Component
public class Knot003Service {

	private final Knoot003NotifikasjonEntityMapper notifikasjonEntityMapper;
	private final KafkaEventProducer kafkaEventProducer;
	private final AltinnConsumer altinnConsumer;

	public Knot003Service(
            Knoot003NotifikasjonEntityMapper notifikasjonEntityMapper,
            KafkaEventProducer kafkaEventProducer,
            AltinnConsumer altinnConsumer
	) {
		this.notifikasjonEntityMapper = notifikasjonEntityMapper;
		this.kafkaEventProducer = kafkaEventProducer;
		this.altinnConsumer = altinnConsumer;
	}

	public void konsumerDistribusjonId(int notifikasjonDistribusjonId) {

		DoknotifikasjonEpost doknotifikasjonEpost;

		try {
			doknotifikasjonEpost = notifikasjonEntityMapper.mapNotifikasjonDistrubisjon(notifikasjonDistribusjonId);
		} catch (Exception exception) {
			log.error("NotifikasjonDistribusjonConsumer kunne ikke hente notifikasjon", exception);
			return;
		}

		if (!validateDistribusjonStatusOgKanal(doknotifikasjonEpost)) {
			String melding = doknotifikasjonEpost.distribusjonStatus == Status.OPPRETTET
					? FEILET_EPOST_UGYLDIG_KANAL
					: FEILET_EPOST_UGYLDIG_STATUS;

			publishStatus(doknotifikasjonEpost, Status.FEILET, melding);
			return;
		}

		try {
			altinnConsumer.sendStandaloneNotificationV3(
					Kanal.EPOST,
					doknotifikasjonEpost.kontakt,
					doknotifikasjonEpost.tekst
			);

			log.info(FERDIGSTILT_NOTIFIKASJON_EPOST + " notifikasjonDistribusjonId={}", notifikasjonDistribusjonId);
		} catch (SoapFaultClientException soapFault) {
			log.error("Knot003 NotifikasjonDistribusjonConsumer har mottatt faultmelding fra altinn fault reason={}", soapFault.getFaultStringOrReason(), soapFault);
			publishStatus(doknotifikasjonEpost, Status.FEILET, soapFault.getFaultStringOrReason());
			return;

		} catch (AltinnFunctionalException altinnFunctionalException) {
			log.error("Knot003 NotifikasjonDistribusjonConsumer funksjonell feil ved kall mot altinn: feilmelding={}", altinnFunctionalException.getMessage(), altinnFunctionalException);
			publishStatus(doknotifikasjonEpost, Status.FEILET, altinnFunctionalException.getMessage());
			return;
		} catch (Exception unknownException) {
			log.error("Knot003 NotifikasjonDistribusjonConsumer ukjent exception", unknownException);
			publishStatus(
					doknotifikasjonEpost,
					Status.FEILET,
					Optional.of(unknownException).map(Exception::getMessage).orElse("")
			);
			return;
		}


		try {
			notifikasjonEntityMapper.updateEntity(
					notifikasjonDistribusjonId,
					doknotifikasjonEpost.bestillerId
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

		publishStatus(doknotifikasjonEpost, Status.FERDIGSTILT, FERDIGSTILT_NOTIFIKASJON_EPOST);

	}

	private boolean validateDistribusjonStatusOgKanal(DoknotifikasjonEpost doknotifikasjonEpost) {
		return doknotifikasjonEpost.getDistribusjonStatus().equals(Status.OPPRETTET)
				&& doknotifikasjonEpost.kanal.equals(Kanal.EPOST);
	}


	private void publishStatus(DoknotifikasjonEpost doknotifikasjonEpost, Status status, String melding) {
		kafkaEventProducer.publish(
				KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
				doknotifikasjonEpost.notifikasjonDistribusjonId,
				DoknotifikasjonStatus.newBuilder()
						.setBestillerId(doknotifikasjonEpost.bestillerId)
						.setBestillingsId(doknotifikasjonEpost.bestillingsId)
						.setStatus(status.name())
						.setMelding(melding)
						.setDistribusjonId(Long.valueOf(doknotifikasjonEpost.getNotifikasjonDistribusjonId()))
						.build(),
				System.currentTimeMillis()
		);
	}

}
