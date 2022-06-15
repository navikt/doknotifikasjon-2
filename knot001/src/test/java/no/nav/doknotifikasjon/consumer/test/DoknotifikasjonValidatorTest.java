package no.nav.doknotifikasjon.consumer.test;

import no.nav.doknotifikasjon.consumer.DoknotifikasjonValidator;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjon;
import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjonWithInvalidAntallRenotifikasjoner;
import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjonWithInvalidFnr;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {DoknotifikasjonValidator.class})
class DoknotifikasjonValidatorTest {

	@Autowired
	DoknotifikasjonValidator doknotifikasjonValidator;

	@MockBean
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
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER, null
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
}