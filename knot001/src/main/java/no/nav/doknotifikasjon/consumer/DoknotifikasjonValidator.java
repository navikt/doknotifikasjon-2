package no.nav.doknotifikasjon.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.springframework.stereotype.Component;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.ANTALL_RENOTIFIKASJONER_REQUIRES_RENOTIFIKASJON_INTERVALL;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class DoknotifikasjonValidator {

	private final KafkaStatusEventProducer statusProducer;

	private static final int MAX_STRING_SIZE_LARGE = 4000;
	private static final int MAX_STRING_SIZE_MEDIUM = 100;
	private static final int MAX_SIZE_RENOTIFIKASJONER = 30;

	DoknotifikasjonValidator(KafkaStatusEventProducer statusProducer) {
		this.statusProducer = statusProducer;
	}

	public void validate(Doknotifikasjon doknotifikasjon) {
		log.info("Starter validering av Doknotifikasjon-melding med bestillingsId={}", doknotifikasjon.getBestillingsId());

		validateString(doknotifikasjon, doknotifikasjon.getBestillingsId(), MAX_STRING_SIZE_MEDIUM, "bestillingsId");
		validateString(doknotifikasjon, doknotifikasjon.getBestillerId(), MAX_STRING_SIZE_MEDIUM, "bestillerId");
		validateFoedselsnummer(doknotifikasjon);
		validateString(doknotifikasjon, doknotifikasjon.getTittel(), MAX_STRING_SIZE_MEDIUM, "tittel");
		validateString(doknotifikasjon, doknotifikasjon.getEpostTekst(), MAX_STRING_SIZE_LARGE, "epostTekst");
		validateString(doknotifikasjon, doknotifikasjon.getSmsTekst(), MAX_STRING_SIZE_LARGE, "smsTekst");

		validateNumberForSnot001(doknotifikasjon, doknotifikasjon.getAntallRenotifikasjoner(), "antallRenotifikasjoner");
		validateNumberForSnot001(doknotifikasjon, doknotifikasjon.getRenotifikasjonIntervall(), "renotifikasjonIntervall");
		validateRenotifikasjoner(doknotifikasjon);
	}

	private void validateFoedselsnummer(Doknotifikasjon doknotifikasjon) {
		var feilmelding = "";
		var fnr = doknotifikasjon.getFodselsnummer();

		if (isBlank(fnr)) {
			feilmelding = "fødselsnummer må være satt";
		} else if (fnr.trim().length() != 11) {
			feilmelding = "fødselsnummer må være 11 siffer";
		}

		processValidationResult(doknotifikasjon, feilmelding);
	}

	public void validateString(Doknotifikasjon doknotifikasjon, String string, int maxLength, String fieldName) {
		var feilmelding = "";

		if (isBlank(string)) {
			feilmelding = String.format("%s må være satt", fieldName);
		} else if (string.length() > maxLength) {
			feilmelding = String.format("%s kan ikke være lenger enn %s tegn", fieldName, maxLength);
		}

		processValidationResult(doknotifikasjon, feilmelding);
	}

	public void validateNumberForSnot001(
			Doknotifikasjon doknotifikasjon,
			Integer numberToValidate,
			String fieldName
	) {
		if (numberToValidate != null && numberToValidate > MAX_SIZE_RENOTIFIKASJONER) {
			processValidationResult(doknotifikasjon, String.format("%s kan ikke være større enn %s", fieldName, MAX_SIZE_RENOTIFIKASJONER));
		}
	}

	public void validateRenotifikasjoner(Doknotifikasjon doknotifikasjon) {
		if ((doknotifikasjon.getAntallRenotifikasjoner() != null && doknotifikasjon.getAntallRenotifikasjoner() > 0) &&
				!(doknotifikasjon.getRenotifikasjonIntervall() != null && doknotifikasjon.getRenotifikasjonIntervall() > 0)) {
			processValidationResult(doknotifikasjon, ANTALL_RENOTIFIKASJONER_REQUIRES_RENOTIFIKASJON_INTERVALL);
		}
	}

	private void processValidationResult(Doknotifikasjon doknotifikasjon, String feilmelding) {
		if(!feilmelding.isEmpty()) {
			statusProducer.publishDoknotifikasjonStatusFeilet(
					doknotifikasjon.getBestillingsId(),
					doknotifikasjon.getBestillerId(),
					feilmelding,
					null
			);

			throw new InvalidAvroSchemaFieldException(String.format("Doknotifikasjon-melding med bestillingsId=%s feilet validering: %s.",
					doknotifikasjon.getBestillingsId(), feilmelding));
		}
	}
}
