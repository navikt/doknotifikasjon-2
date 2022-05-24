package no.nav.doknotifikasjon.consumer.test;

import no.nav.doknotifikasjon.consumer.NotifikasjonValidator;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjonWithInvalidAntallRenotifikasjoner;
import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjonWithoutEpostOrSms;
import static no.nav.doknotifikasjon.consumer.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_MUST_HAVE_EITHER_MOBILTELEFONNUMMER_OR_EPOSTADESSE_AS_SETT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {NotifikasjonValidator.class})
class DoknotifikasjonValidatorTest {

	@Autowired
	NotifikasjonValidator doknotifikasjonValidator;

	@MockBean
	KafkaStatusEventProducer statusProducer;

	@Test
	void shouldValidateAvroSchemaWhenSendingValidAvroSchema() {
		doknotifikasjonValidator.validate(createNotifikasjon());
	}

	@Test
	void shouldFailValidateAvroSchemaWhenSendingInvalidAvroSchema() {
		NotifikasjonMedkontaktInfo notifikasjon = createDoknotifikasjonWithInvalidAntallRenotifikasjoner();
		assertThrows(InvalidAvroSchemaFieldException.class, () -> doknotifikasjonValidator.validate(notifikasjon));

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER, null
		);
	}

	@Test
	void shouldFailValidateWhenMissingEpostadresseAndMobiltelefonnummer() {
		NotifikasjonMedkontaktInfo notifikasjon = createDoknotifikasjonWithoutEpostOrSms();
		assertThrows(InvalidAvroSchemaFieldException.class, () -> doknotifikasjonValidator.validate(notifikasjon));

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), FEILET_MUST_HAVE_EITHER_MOBILTELEFONNUMMER_OR_EPOSTADESSE_AS_SETT, null
		);
	}

	@Test
	void shouldFailValidateNumberWhenFieldIsNegative() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				doknotifikasjonValidator.validateNumber(notifikasjon, -200, "number")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), "påkrevd felt number kan ikke være negativ", null
		);
	}

	@Test
	void shouldFailValidateStringWhenFieldIsNull() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				doknotifikasjonValidator.validateString(notifikasjon, null, 4000, "string")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), "påkrevd felt string ikke satt", null
		);
	}

	@Test
	void shouldFailValidateStringWhenFieldIsEmpty() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				doknotifikasjonValidator.validateString(notifikasjon, "         ", 4000, "string")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), "påkrevd felt string ikke satt", null
		);
	}

	@Test
	void shouldFailValidateStringWhenFieldIsToShort() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				doknotifikasjonValidator.validateString(notifikasjon, "Test", 2, "string")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), "påkrevd felt string har for lang string lengde", null
		);
	}

	@Test
	void shouldValidateStringWhenParametersIsCorrect() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		doknotifikasjonValidator.validateString(notifikasjon, "Test", 10, "string");
	}

	@Test
	void shouldValidateNumberForSnot001WhenParametersIsCorrect() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		doknotifikasjonValidator.validateNumberForSnot001(notifikasjon, 30, "String");
	}


	@Test
	void shouldFailValidateNumberSnot001WhenFieldIsToGreaterThen30() {
		NotifikasjonMedkontaktInfo notifikasjon = createNotifikasjon();
		assertThrows(InvalidAvroSchemaFieldException.class, () ->
				doknotifikasjonValidator.validateNumberForSnot001(notifikasjon, 100, "string")
		);

		verify(statusProducer).publishDoknotifikasjonStatusFeilet(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), "Felt string kan ikke være støre enn 30", null
		);
	}
}