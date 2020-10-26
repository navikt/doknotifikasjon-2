package no.nav.doknotifikasjon.consumer.test;

import no.nav.doknotifikasjon.consumer.DoknotifikasjonValidator;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.kafka.KafkaDoknotifikasjonStatusProducer;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjon;
import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjonWithInvalidAntallRenotifikasjoner;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {DoknotifikasjonValidator.class})
class DoknotifikasjonValidatorTest {

    @Autowired
    DoknotifikasjonValidator doknotifikasjonValidator;

    @MockBean
    KafkaDoknotifikasjonStatusProducer statusProducer;

    @Test
    void shouldValidateAvroSchemaWhenSendingValidAvroSchema() {
        doknotifikasjonValidator.validate(createDoknotifikasjon());
    }

    @Test
    void shouldFailValidateAvroSchemaWhenSendingInvalidAvroSchema() {
        Doknotifikasjon doknotifikasjon = createDoknotifikasjonWithInvalidAntallRenotifikasjoner();
        assertThrows(InvalidAvroSchemaFieldException.class, () -> doknotifikasjonValidator.validate(doknotifikasjon));

        verify(statusProducer).publishDoknotikfikasjonStatusFeilet(
                doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER, null
        );
    }

    @Test
    void shouldFailValidateNumberWhenFieldIsNegative() {
        Doknotifikasjon doknotifikasjon = createDoknotifikasjon();
        assertThrows(InvalidAvroSchemaFieldException.class, () ->
                doknotifikasjonValidator.validateNumber(doknotifikasjon, -200, "number")
        );

        verify(statusProducer).publishDoknotikfikasjonStatusFeilet(
                doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), "påkrevd felt number kan ikke være negativ", null
        );
    }

    @Test
    void shouldFailValidateStringWhenFieldIsNull() {
        Doknotifikasjon doknotifikasjon = createDoknotifikasjon();
        assertThrows(InvalidAvroSchemaFieldException.class, () ->
                doknotifikasjonValidator.validateString(doknotifikasjon, null, "string")
        );

        verify(statusProducer).publishDoknotikfikasjonStatusFeilet(
                doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), "påkrevd felt string ikke satt", null
        );
    }

    @Test
    void shouldFailValidateStringWhenFieldIsEmpty() {
        Doknotifikasjon doknotifikasjon = createDoknotifikasjon();
        assertThrows(InvalidAvroSchemaFieldException.class, () ->
                doknotifikasjonValidator.validateString(doknotifikasjon, "         ", "string")
        );

        verify(statusProducer).publishDoknotikfikasjonStatusFeilet(
                doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), "påkrevd felt string ikke satt", null
        );
    }
}