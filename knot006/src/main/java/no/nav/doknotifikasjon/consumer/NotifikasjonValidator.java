package no.nav.doknotifikasjon.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo;
import org.springframework.stereotype.Component;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.ANTALL_RENOTIFIKASJONER_REQUIRES_RENOTIFIKASJON_INTERVALL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.MOBILTELEFONNUMMER_OR_EPOSTADESSE_MUST_BE_SET;
import static org.apache.logging.log4j.util.Strings.isBlank;

@Slf4j
@Component
public class NotifikasjonValidator {

	private final KafkaStatusEventProducer statusProducer;

	private static final int MAX_STRING_SIZE_LARGE = 4000;
	private static final int MAX_STRING_SIZE_MEDIUM = 100;
	private static final int MAX_SIZE_RENOTIFIKASJONER = 30;

	NotifikasjonValidator(KafkaStatusEventProducer statusProducer) {
		this.statusProducer = statusProducer;
	}

	public void validate(NotifikasjonMedkontaktInfo notifikasjon) {
		log.info("Starter validering av NotifikasjonMedkontaktInfo-melding med bestillingsId={}", notifikasjon.getBestillingsId());

		validateString(notifikasjon, notifikasjon.getBestillingsId(), MAX_STRING_SIZE_MEDIUM, "BestillingsId");
		validateString(notifikasjon, notifikasjon.getBestillerId(), MAX_STRING_SIZE_MEDIUM, "BestillerId");
		validateFoedselsnummer(notifikasjon);
		validateString(notifikasjon, notifikasjon.getTittel(), MAX_STRING_SIZE_MEDIUM, "Tittel");
		validateString(notifikasjon, notifikasjon.getEpostTekst(), MAX_STRING_SIZE_LARGE, "EpostTekst");
		validateString(notifikasjon, notifikasjon.getSmsTekst(), MAX_STRING_SIZE_LARGE, "SmsTekst");

		validateNumberForSnot001(notifikasjon, notifikasjon.getAntallRenotifikasjoner(), "antallRenotifikasjoner");
		validateNumberForSnot001(notifikasjon, notifikasjon.getRenotifikasjonIntervall(), "renotifikasjonIntervall");

		validateTelefonnummerOgEpost(notifikasjon);
		validateRenotifikasjoner(notifikasjon);
	}

	public void validateString(NotifikasjonMedkontaktInfo notifikasjon, String string, int maxLength, String fieldName) {
		var feilmelding = new StringBuilder();

		if (isBlank(string)) {
			feilmelding.append(fieldName).append(" må være satt");
		} else if (string.length() > maxLength) {
			feilmelding.append(fieldName).append(" kan ikke være lenger enn ").append(maxLength).append(" tegn");
		}

		processValidationResult(notifikasjon, feilmelding.toString());
	}

	private void validateFoedselsnummer(NotifikasjonMedkontaktInfo notifikasjon) {
		var feilmelding = new StringBuilder();
		var fnr = notifikasjon.getFodselsnummer();

		if (isBlank(fnr)) {
			feilmelding.append("fødselsnummer må være ").append("satt");
		} else if (fnr.trim().length() != 11) {
			feilmelding.append("fødselsnummer må være ").append("11 siffer");
		}

		processValidationResult(notifikasjon, feilmelding.toString());
	}

	public void validateNumberForSnot001(
			NotifikasjonMedkontaktInfo notifikasjon,
			Integer numberToValidate,
			String fieldName
	) {
		if (numberToValidate != null && numberToValidate > MAX_SIZE_RENOTIFIKASJONER) {
			processValidationResult(notifikasjon, String.format("%s kan ikke være større enn %s", fieldName, MAX_SIZE_RENOTIFIKASJONER));
		}
	}

	private void validateTelefonnummerOgEpost(NotifikasjonMedkontaktInfo notifikasjon) {

		if (isBlank(notifikasjon.getEpostadresse()) && isBlank(notifikasjon.getMobiltelefonnummer())) {
			processValidationResult(notifikasjon, MOBILTELEFONNUMMER_OR_EPOSTADESSE_MUST_BE_SET);
		}
	}

	public void validateRenotifikasjoner(NotifikasjonMedkontaktInfo notifikasjon) {
		if ((notifikasjon.getAntallRenotifikasjoner() != null && notifikasjon.getAntallRenotifikasjoner() > 0) &&
				!(notifikasjon.getRenotifikasjonIntervall() != null && notifikasjon.getRenotifikasjonIntervall() > 0)) {
			processValidationResult(notifikasjon, ANTALL_RENOTIFIKASJONER_REQUIRES_RENOTIFIKASJON_INTERVALL);
		}
	}

	private void processValidationResult(NotifikasjonMedkontaktInfo notifikasjon, String feilmelding) {
		if(!feilmelding.isEmpty()) {
			statusProducer.publishDoknotifikasjonStatusFeilet(
					notifikasjon.getBestillingsId(),
					notifikasjon.getBestillerId(),
					feilmelding,
					null
			);

			throw new InvalidAvroSchemaFieldException(String.format("NotifikasjonMedkontaktInfo-melding med bestillingsId=%s feilet validering: %s.",
					notifikasjon.getBestillingsId(), feilmelding));
		}
	}

}
