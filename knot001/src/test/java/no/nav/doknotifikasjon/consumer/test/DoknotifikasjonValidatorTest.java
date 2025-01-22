package no.nav.doknotifikasjon.consumer.test;

import no.nav.doknotifikasjon.consumer.DoknotifikasjonValidator;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjon;
import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjonWithInvalidAntallRenotifikasjoner;
import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjonWithInvalidFnr;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.ANTALL_RENOTIFIKASJONER_REQUIRES_RENOTIFIKASJON_INTERVALL;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {DoknotifikasjonValidator.class})
class DoknotifikasjonValidatorTest {

	@Autowired
	DoknotifikasjonValidator doknotifikasjonValidator;

	@MockitoBean
	KafkaStatusEventProducer statusProducer;

	@Test
	void shouldValidateAvroSchemaWhenSendingValidAvroSchema() {
		doknotifikasjonValidator.validate(createDoknotifikasjon());
	}

	@Test
	void shouldFailValidateAvroSchemaWhenSendingInvalidAvroSchema() {
		Doknotifikasjon doknotifikasjon = createDoknotifikasjonWithInvalidAntallRenotifikasjoner();
		assertThrows(InvalidAvroSchemaFieldException.class, () -> doknotifikasjonValidator.validate(doknotifikasjon));

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), ANTALL_RENOTIFIKASJONER_REQUIRES_RENOTIFIKASJON_INTERVALL, null
		);
	}

	@Test
	void shouldFelAvroSchemaWhenSendingAvroSchemaWithInvalidFnr() {
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				doknotifikasjonValidator.validate(createDoknotifikasjonWithInvalidFnr())
		);
	}

	@Test
	void shouldFailValidateStringWhenFieldIsNull() {
		Doknotifikasjon doknotifikasjon = createDoknotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				doknotifikasjonValidator.validateString(doknotifikasjon, null, 4000, "string")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), "string må være satt", null
		);
	}

	@Test
	void shouldFailValidateStringWhenFieldIsEmpty() {
		Doknotifikasjon doknotifikasjon = createDoknotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				doknotifikasjonValidator.validateString(doknotifikasjon, "         ", 4000, "string")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), "string må være satt", null
		);
	}

	@Test
	void shouldFailValidateStringWhenFieldIsToShort() {
		Doknotifikasjon doknotifikasjon = createDoknotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				doknotifikasjonValidator.validateString(doknotifikasjon, "Test", 2, "string")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), "string kan ikke være lenger enn 2 tegn", null
		);
	}

	@Test
	void shouldValidateStringWhenParametersIsCorrect() {
		Doknotifikasjon doknotifikasjon = createDoknotifikasjon();
		doknotifikasjonValidator.validateString(doknotifikasjon, "Test", 10, "string");
	}


	@Test
	void shouldValidateNumberForSnot001WhenParametersIsCorrect() {
		Doknotifikasjon doknotifikasjon = createDoknotifikasjon();
		doknotifikasjonValidator.validateNumberForSnot001(doknotifikasjon, 30, "String");
	}


	@Test
	void shouldFailValidateNumberSnot001WhenFieldIsToGreaterThen30() {
		Doknotifikasjon doknotifikasjon = createDoknotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				doknotifikasjonValidator.validateNumberForSnot001(doknotifikasjon, 100, "string")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), "string kan ikke være større enn 30", null
		);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(ints = {0, -2})
	void shouldFailValidateOnInvalidRenotifikasjonIntervall(Integer intervall) {
		Doknotifikasjon doknotifikasjon = createDoknotifikasjon();
		doknotifikasjon.setAntallRenotifikasjoner(5);
		doknotifikasjon.setRenotifikasjonIntervall(intervall);
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				doknotifikasjonValidator.validateRenotifikasjoner(doknotifikasjon)
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), "antallRenotifikasjoner krever at renotifikasjonIntervall er satt", null
		);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(ints = 0)
	void shouldValidateOnAntallRenotifikasjonerNullOrZero(Integer antallRenotifikasjoner) {
		Doknotifikasjon doknotifikasjon = createDoknotifikasjon();
		doknotifikasjon.setAntallRenotifikasjoner(antallRenotifikasjoner);
		doknotifikasjon.setRenotifikasjonIntervall(5);

		doknotifikasjonValidator.validateRenotifikasjoner(doknotifikasjon);
	}
}