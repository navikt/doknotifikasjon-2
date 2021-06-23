package no.nav.doknotifikasjon.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_MUST_HAVE_EITHER_MOBILTELEFONNUMMER_OR_EPOSTADESSE_AS_SETT;
import static org.apache.logging.log4j.util.Strings.*;

@Slf4j
@Component
public class NotifikasjonValidator {

	private final KafkaStatusEventProducer statusProducer;

	private static final int MAX_STRING_SIZE_LARGE = 4000;
	private static final int MAX_STRING_SIZE_MEDIUM = 100;
	private static final int MAX_STRING_SIZE_SMALL = 40;

	@Inject
	NotifikasjonValidator(KafkaStatusEventProducer statusProducer) {
		this.statusProducer = statusProducer;
	}

	public void validate(NotifikasjonMedkontaktInfo notifikasjon) {
		log.info("Begynner med validering av AVRO skjema med bestillingsId={}", notifikasjon.getBestillingsId());

		this.validateString(notifikasjon, notifikasjon.getBestillingsId(), MAX_STRING_SIZE_MEDIUM, "BestillingsId");
		this.validateString(notifikasjon, notifikasjon.getBestillerId(), MAX_STRING_SIZE_MEDIUM, "BestillerId");
		this.validateString(notifikasjon, notifikasjon.getFodselsnummer(), MAX_STRING_SIZE_SMALL, "Fodselsnummer");
		this.validateString(notifikasjon, notifikasjon.getTittel(), MAX_STRING_SIZE_SMALL, "Tittel");
		this.validateString(notifikasjon, notifikasjon.getEpostTekst(), MAX_STRING_SIZE_LARGE, "EpostTekst");
		this.validateString(notifikasjon, notifikasjon.getSmsTekst(), MAX_STRING_SIZE_LARGE, "SmsTekst");
		this.validateNumberForSnot001(notifikasjon, notifikasjon.getAntallRenotifikasjoner(), "antallRenotifikasjoner");
		this.validateNumberForSnot001(notifikasjon, notifikasjon.getRenotifikasjonIntervall(), "renotifikasjonIntervall");

		if (isBlank(notifikasjon.getEpostadresse()) && isBlank(notifikasjon.getMobiltelefonnummer())) {
			statusProducer.publishDoknotikfikasjonStatusFeilet(
					notifikasjon.getBestillingsId(),
					notifikasjon.getBestillerId(),
					FEILET_MUST_HAVE_EITHER_MOBILTELEFONNUMMER_OR_EPOSTADESSE_AS_SETT,
					null
			);

			throw new InvalidAvroSchemaFieldException(String.format("Feilet med å validere DoknotifikasjonMedKontaktInfo AVRO skjema med bestillingsId=%s. Feilmelding: %s. ",
					notifikasjon.getBestillingsId(), FEILET_MUST_HAVE_EITHER_MOBILTELEFONNUMMER_OR_EPOSTADESSE_AS_SETT));
		}

		if ((notifikasjon.getAntallRenotifikasjoner() != null && notifikasjon.getAntallRenotifikasjoner() > 0) &&
				!(notifikasjon.getRenotifikasjonIntervall() != null && notifikasjon.getRenotifikasjonIntervall() > 0)) {
			statusProducer.publishDoknotikfikasjonStatusFeilet(
					notifikasjon.getBestillingsId(),
					notifikasjon.getBestillerId(),
					FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER,
					null
			);

			throw new InvalidAvroSchemaFieldException(String.format("Feilet med å validere Doknotifikasjon AVRO skjema med bestillingsId=%s. Feilmelding: %s. ",
					notifikasjon.getBestillingsId(), FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER));
		}
	}

	public void validateString(NotifikasjonMedkontaktInfo notifikasjon, String string, int maxLength, String fieldName) {
		if (string == null || string.trim().isEmpty() || string.length() > maxLength) {
			String addedString = string == null || string.trim().isEmpty() ? " ikke satt" : " har for lang string lengde";

			statusProducer.publishDoknotikfikasjonStatusFeilet(
					notifikasjon.getBestillingsId(),
					notifikasjon.getBestillerId(),
					"påkrevd felt " + fieldName + addedString,
					null
			);
			throw new InvalidAvroSchemaFieldException("AVRO skjema Doknotifikasjon er ikke gylding for bestilling med bestillingsId: " + notifikasjon.getBestillingsId());
		}
	}

	public void validateNumber(NotifikasjonMedkontaktInfo notifikasjon, Integer number, String fieldName) {
		if (number != null && number < 0) {
			statusProducer.publishDoknotikfikasjonStatusFeilet(
					notifikasjon.getBestillingsId(),
					notifikasjon.getBestillerId(),
					"påkrevd felt " + fieldName + " kan ikke være negativ",
					null
			);
			throw new InvalidAvroSchemaFieldException("AVRO skjema Doknotifikasjon er ikke gylding for bestilling med bestillingsId: " + notifikasjon.getBestillingsId());
		}
	}

	/* Denne funksjonen vil forhindre at feltet antallRenotifikasjoner og renotifikasjonIntervall vil aldri bli støre enn 30*/
	public void validateNumberForSnot001(
			NotifikasjonMedkontaktInfo notifikasjon,
			Integer numberToValidate,
			String fieldName
	){
		if (numberToValidate != null && numberToValidate > 30) {
			statusProducer.publishDoknotikfikasjonStatusFeilet(
					notifikasjon.getBestillingsId(),
					notifikasjon.getBestillerId(),
					"Felt " + fieldName + " kan ikke være støre enn 30",
					null
			);
			throw new InvalidAvroSchemaFieldException("AVRO skjema Doknotifikasjon er ikke gylding for bestilling med bestillingsId=" + notifikasjon.getBestillingsId());
		}
	}
}
