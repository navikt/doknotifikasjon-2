package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import org.junit.Test;

import static no.nav.doknotifikasjon.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DoknotifikasjonStatusValidatorTest {

	private final DoknotifikasjonStatusValidator doknotifikasjonStatusValidator = new DoknotifikasjonStatusValidator();

	@Test
	public void shouldValidateInput() {
		doknotifikasjonStatusValidator.validateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLINGS_ID, STATUS_OPPRETTET, MELDING, DISTRIBUSJON_ID));
	}

	@Test
	public void shouldValidateWithoutDistribusjonIdSet() {
		doknotifikasjonStatusValidator.validateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLINGS_ID, STATUS_OPPRETTET, MELDING, null));
	}

	@Test
	public void shouldNotValidateWithoutBestillingsId() {
		DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
				doknotifikasjonStatusValidator.validateInput(new DoknotifikasjonStatusTo(null, BESTILLINGS_ID, STATUS_OPPRETTET, MELDING, DISTRIBUSJON_ID)));
		assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic privat-dok-notifikasjon-status har tom verdi for bestillingsId.", exception.getMessage());
	}

	@Test
	public void shouldNotValidateWithoutBestillerId() {
		DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
				doknotifikasjonStatusValidator.validateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, null, STATUS_OPPRETTET, MELDING, DISTRIBUSJON_ID)));
		assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic privat-dok-notifikasjon-status har tom verdi for bestillerId.", exception.getMessage());
	}

	@Test
	public void shouldNotValidateWithoutMelding() {
		DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
				doknotifikasjonStatusValidator.validateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLINGS_ID, STATUS_OPPRETTET, null, DISTRIBUSJON_ID)));
		assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic privat-dok-notifikasjon-status har tom verdi for melding.", exception.getMessage());
	}

	@Test
	public void shouldNotValidateWithoutStatus() {
		DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
				doknotifikasjonStatusValidator.validateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLINGS_ID, null, MELDING, DISTRIBUSJON_ID)));
		assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic privat-dok-notifikasjon-status har tom verdi for status.", exception.getMessage());
	}
}