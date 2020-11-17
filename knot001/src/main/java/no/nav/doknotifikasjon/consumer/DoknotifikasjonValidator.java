package no.nav.doknotifikasjon.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER;

@Slf4j
@Component
public class DoknotifikasjonValidator {

	private final KafkaStatusEventProducer statusProducer;

	private static final int MAX_STRING_SIZE_LARGE = 4000;
	private static final int MAX_STRING_SIZE_MEDIUM = 100;
	private static final int MAX_STRING_SIZE_SMALL = 40;

	@Inject
	DoknotifikasjonValidator(KafkaStatusEventProducer statusProducer) {
		this.statusProducer = statusProducer;
	}

	public void validate(Doknotifikasjon doknotifikasjon) {
		log.info("Begynner med validering av AVRO skjema med bestillingsId={}", doknotifikasjon.getBestillingsId());

		this.validateString(doknotifikasjon, doknotifikasjon.getBestillingsId(), MAX_STRING_SIZE_MEDIUM, "BestillingsId");
		this.validateString(doknotifikasjon, doknotifikasjon.getBestillerId(), MAX_STRING_SIZE_MEDIUM, "BestillerId");
		this.validateString(doknotifikasjon, doknotifikasjon.getFodselsnummer(), MAX_STRING_SIZE_SMALL, "Fodselsnummer");
		this.validateString(doknotifikasjon, doknotifikasjon.getTittel(), MAX_STRING_SIZE_SMALL, "Tittel");
		this.validateString(doknotifikasjon, doknotifikasjon.getEpostTekst(), MAX_STRING_SIZE_LARGE, "EpostTekst");
		this.validateString(doknotifikasjon, doknotifikasjon.getSmsTekst(), MAX_STRING_SIZE_LARGE, "SmsTekst");
		this.validateNumber(doknotifikasjon, doknotifikasjon.getAntallRenotifikasjoner(), "AntallRenotifikasjoner");
		this.validateNumber(doknotifikasjon, doknotifikasjon.getRenotifikasjonIntervall(), "RenotifikasjonIntervall");

		if ((doknotifikasjon.getAntallRenotifikasjoner() != null && doknotifikasjon.getAntallRenotifikasjoner() > 0) &&
				!(doknotifikasjon.getRenotifikasjonIntervall() != null && doknotifikasjon.getRenotifikasjonIntervall() > 0)) {
			statusProducer.publishDoknotikfikasjonStatusFeilet(
					doknotifikasjon.getBestillingsId(),
					doknotifikasjon.getBestillerId(),
					FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER,
					null
			);

			throw new InvalidAvroSchemaFieldException(String.format("Feilet med å validere Doknotifikasjon AVRO skjema med bestillingsId=%s. Feilmelding: %s. ",
					doknotifikasjon.getBestillingsId(), FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER));
		}
	}

	public void validateString(Doknotifikasjon doknotifikasjon, String string, int maxLength, String fieldName) {
		if (string == null || string.trim().isEmpty() || string.length() > maxLength) {
			String addedString = string == null || string.trim().isEmpty() ? " ikke satt" : " har for lang string lengde";

			statusProducer.publishDoknotikfikasjonStatusFeilet(
					doknotifikasjon.getBestillingsId(),
					doknotifikasjon.getBestillerId(),
					"påkrevd felt " + fieldName + addedString,
					null
			);
			throw new InvalidAvroSchemaFieldException("AVRO skjema Doknotifikasjon er ikke gylding for bestilling med bestillingsId: " + doknotifikasjon.getBestillingsId());
		}
	}

	public void validateNumber(Doknotifikasjon doknotifikasjon, Integer number, String fieldName) {
		if (number != null && number < 0) {
			statusProducer.publishDoknotikfikasjonStatusFeilet(
					doknotifikasjon.getBestillingsId(),
					doknotifikasjon.getBestillerId(),
					"påkrevd felt " + fieldName + " kan ikke være negativ",
					null
			);
			throw new InvalidAvroSchemaFieldException("AVRO skjema Doknotifikasjon er ikke gylding for bestilling med bestillingsId: " + doknotifikasjon.getBestillingsId());
		}
	}
}
