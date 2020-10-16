package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import org.junit.Test;

import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLER_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLING_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.DISTRIBUSJON_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.MELDING;
import static no.nav.doknotifikasjon.utils.TestUtils.STATUS_OPPRETTET;
import static no.nav.doknotifikasjon.utils.TestUtils.UGYLDIG_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DoknotifikasjonStatusValidatorTest {

    private final DoknotifikasjonStatusValidator doknotifikasjonStatusValidator = new DoknotifikasjonStatusValidator();

    @Test
    public void shouldValidateInput() {
        doknotifikasjonStatusValidator.shouldValidateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLING_ID, STATUS_OPPRETTET, MELDING, DISTRIBUSJON_ID));
    }

    @Test
    public void shouldValidateOkWithoutDistribusjonId() {
        doknotifikasjonStatusValidator.shouldValidateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLING_ID, STATUS_OPPRETTET, MELDING, null));
    }

    @Test
    public void shouldNotValidateWithoutBestillingId() {
        DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
                doknotifikasjonStatusValidator.shouldValidateInput(new DoknotifikasjonStatusTo(null, BESTILLING_ID, STATUS_OPPRETTET, MELDING, DISTRIBUSJON_ID)));
        assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic dok-eksternnotifikasjon-status har tom variabel: bestillingId. ", exception.getMessage());
    }

    @Test
    public void shouldNotValidateWithoutBestillerId() {
        DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
                doknotifikasjonStatusValidator.shouldValidateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, null, STATUS_OPPRETTET, MELDING, DISTRIBUSJON_ID)));
        assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic dok-eksternnotifikasjon-status har tom variabel: bestillerId. ", exception.getMessage());
    }

    @Test
    public void shouldNotValidateWithoutMelding() {
        DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
                doknotifikasjonStatusValidator.shouldValidateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLING_ID, STATUS_OPPRETTET, null, DISTRIBUSJON_ID)));
        assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic dok-eksternnotifikasjon-status har tom variabel: melding. ", exception.getMessage());
    }

    @Test
    public void shouldNotValidateWithoutStatus() {
        DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
                doknotifikasjonStatusValidator.shouldValidateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLING_ID, null, MELDING, DISTRIBUSJON_ID)));
        assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic dok-eksternnotifikasjon-status har tom variabel: status. ", exception.getMessage());
    }

    @Test
    public void shouldNotValidateWithUgyldigStatus() {
        DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
                doknotifikasjonStatusValidator.shouldValidateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLING_ID, UGYLDIG_STATUS, MELDING, DISTRIBUSJON_ID)));
        assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic dok-eksternnotifikasjon-status har ugyldig status: OPRETET.", exception.getMessage());
    }
}