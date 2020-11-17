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

	@Inject
	DoknotifikasjonValidator(KafkaStatusEventProducer statusProducer) {
		this.statusProducer = statusProducer;
	}

	public void validate(Doknotifikasjon doknotifikasjon) {
		log.info("Begynner med validering av AVRO skjema med bestillingsId={}", doknotifikasjon.getBestillingsId());

		this.validateString(doknotifikasjon, doknotifikasjon.getBestillingsId(), "BestillingsId");
		this.validateString(doknotifikasjon, doknotifikasjon.getBestillerId(), "BestillerId");
		this.validateString(doknotifikasjon, doknotifikasjon.getFodselsnummer(), "Fodselsnummer");
		this.validateString(doknotifikasjon, doknotifikasjon.getTittel(), "Tittel");
		this.validateString(doknotifikasjon, doknotifikasjon.getEpostTekst(), "EpostTekst");
		this.validateString(doknotifikasjon, doknotifikasjon.getSmsTekst(), "SmsTekst");
		this.validateNumber(doknotifikasjon, doknotifikasjon.getAntallRenotifikasjoner(), "AntallRenotifikasjoner");
		this.validateNumber(doknotifikasjon, doknotifikasjon.getRenotifikasjonIntervall(), "RenotifikasjonIntervall");
		this.validateNumber(doknotifikasjon, doknotifikasjon.getSikkerhetsnivaa(), "Sikkerhetsnivaa");

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

	public void validateString(Doknotifikasjon doknotifikasjon, String string, String fieldName) {
		if (string == null || string.trim().isEmpty()) {
			statusProducer.publishDoknotikfikasjonStatusFeilet(
					doknotifikasjon.getBestillingsId(),
					doknotifikasjon.getBestillerId(),
					"påkrevd felt " + fieldName + " ikke satt",
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
