package no.nav.doknotifikasjon.consumer.test;

import no.nav.doknotifikasjon.consumer.NotifikasjonValidator;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjonWithInvalidAntallRenotifikasjoner;
import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjonWithoutEpostOrSms;
import static no.nav.doknotifikasjon.consumer.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.ANTALL_RENOTIFIKASJONER_REQUIRES_RENOTIFIKASJON_INTERVALL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.MOBILTELEFONNUMMER_OR_EPOSTADESSE_MUST_BE_SET;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {NotifikasjonValidator.class})
class NotifikasjonValidatorTest {

	@Autowired
	NotifikasjonValidator notifikasjonValidator;

	@MockBean
	KafkaStatusEventProducer statusProducer;

	@Test
	void shouldValidateAvroSchemaWhenSendingValidAvroSchema() {
		notifikasjonValidator.validate(createNotifikasjon());
	}

	@Test
	void shouldFailValidateAvroSchemaWhenSendingInvalidAvroSchema() {
		NotifikasjonMedkontaktInfo notifikasjon = createDoknotifikasjonWithInvalidAntallRenotifikasjoner();
		assertThrows(InvalidAvroSchemaFieldException.class, () -> notifikasjonValidator.validate(notifikasjon));

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), ANTALL_RENOTIFIKASJONER_REQUIRES_RENOTIFIKASJON_INTERVALL, null
		);
	}

	@Test
	void shouldFailValidateWhenMissingEpostadresseAndMobiltelefonnummer() {
		NotifikasjonMedkontaktInfo notifikasjon = createDoknotifikasjonWithoutEpostOrSms();
		assertThrows(InvalidAvroSchemaFieldException.class, () -> notifikasjonValidator.validate(notifikasjon));

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), MOBILTELEFONNUMMER_OR_EPOSTADESSE_MUST_BE_SET, null
		);
	}

	@Test
	void shouldFailValidateStringWhenFieldIsNull() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				notifikasjonValidator.validateString(notifikasjon, null, 4000, "string")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), "string må være satt", null
		);
	}

	@Test
	void shouldFailValidateStringWhenFieldIsEmpty() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				notifikasjonValidator.validateString(notifikasjon, "         ", 4000, "string")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), "string må være satt", null
		);
	}

	@Test
	void shouldFailValidateStringWhenFieldIsTooLong() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				notifikasjonValidator.validateString(notifikasjon, "Test", 2, "string")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), "string kan ikke være lenger enn 2 tegn", null
		);
	}

	@Test
	void shouldValidateStringWhenParametersIsCorrect() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		notifikasjonValidator.validateString(notifikasjon, "Test", 10, "string");
	}

	@Test
	void shouldValidateNumberForSnot001WhenParametersIsCorrect() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		notifikasjonValidator.validateNumberForSnot001(notifikasjon, 30, "String");
	}


	@Test
	void shouldFailValidateNumberSnot001WhenFieldIsToGreaterThen30() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				notifikasjonValidator.validateNumberForSnot001(notifikasjon, 100, "string")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), "string kan ikke være større enn 30", null
		);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(ints = {0, -2})
	void shouldFailValidateOnInvalidRenotifikasjonIntervall(Integer intervall) {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		notifikasjon.setAntallRenotifikasjoner(5);
		notifikasjon.setRenotifikasjonIntervall(intervall);
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				notifikasjonValidator.validateRenotifikasjoner(notifikasjon)
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), "antallRenotifikasjoner krever at renotifikasjonIntervall er satt", null
		);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(ints = 0)
	void shouldValidateOnAntallRenotifikasjonerNullOrZero(Integer antallRenotifikasjoner) {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		notifikasjon.setAntallRenotifikasjoner(antallRenotifikasjoner);
		notifikasjon.setRenotifikasjonIntervall(5);

		notifikasjonValidator.validateRenotifikasjoner(notifikasjon);
	}
}