package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.kafka.KafkaTopics;
import no.nav.doknotifikasjon.kodeverk.Status;
import org.springframework.stereotype.Component;

import java.util.List;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_FUNCTIONAL_EXCEPTION_DIGDIR_KRR_PROXY;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_FUNCTIONAL_EXCEPTION_SIKKERHETSNIVAA;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SIKKERHETSNIVAA;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_TECHNICAL_EXCEPTION_DATABASE;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT;
import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;
import static no.nav.doknotifikasjon.kodeverk.Status.INFO;

@Component
@Slf4j
public class Validator {

	private static final List<String> feilmeldingerSomIkkeSkalBliProsessert = List.of(
			FEILET_FUNCTIONAL_EXCEPTION_SIKKERHETSNIVAA,
			FEILET_SIKKERHETSNIVAA,
			FEILET_FUNCTIONAL_EXCEPTION_DIGDIR_KRR_PROXY,
			FEILET_TECHNICAL_EXCEPTION_DATABASE,
			FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET,
			FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT,
			FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION
	);

	public void validateInput(DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
		validateField(doknotifikasjonStatusTo.getBestillingsId(), "bestillingsId");
		validateField(doknotifikasjonStatusTo.getBestillerId(), "bestillerId");
		validateField(doknotifikasjonStatusTo.getMelding(), "melding");
		validateStatus(doknotifikasjonStatusTo.getStatus());
	}

	private void validateField(String field, String fieldName) {
		if (field == null || field.trim().isEmpty()) {
			throw new DoknotifikasjonValidationException(String.format("Valideringsfeil i knot004: Hendelse på kafka-topic " +
					"%s har tom verdi for %s.", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS, fieldName));
		}
	}

	private void validateStatus(Status status) {
		if (status == null) {
			throw new DoknotifikasjonValidationException(String.format("Valideringsfeil i knot004: Hendelse på kafka-topic " +
					"%s har tom verdi for status.", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS));
		}
	}

	public boolean erStatusInfoEllerFeiletMedSpesiellFeilmelding(Status status, String melding) {
		if (INFO.equals(status)) {
			log.info("Knot004 Melding med status {} skal ikke oppdatere status. Avslutter behandlingen.", INFO);
			return true;
		}

		if (FEILET.equals(status)) {
			if (feilmeldingerSomIkkeSkalBliProsessert.contains(melding)) {
				log.info("Melding med status FEILET og melding={} skal ikke oppdatere status i Knot004. Avslutter behandlingen.", melding);
				return true;
			}
		}
		return false;
	}
}
