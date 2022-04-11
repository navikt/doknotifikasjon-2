package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import org.junit.jupiter.api.Test;

import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;
import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLER_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLINGS_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.DISTRIBUSJON_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.MELDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DoknotifikasjonStatusValidatorTest {

	private final DoknotifikasjonStatusValidator doknotifikasjonStatusValidator = new DoknotifikasjonStatusValidator();

	@Test
	public void shouldValidateInput() {
		doknotifikasjonStatusValidator.validateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLINGS_ID, OPPRETTET, MELDING, DISTRIBUSJON_ID));
	}

	@Test
	public void shouldValidateWithoutDistribusjonIdSet() {
		doknotifikasjonStatusValidator.validateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLINGS_ID, OPPRETTET, MELDING, null));
	}

	@Test
	public void shouldNotValidateWithoutBestillingsId() {
		DoknotifikasjonStatusTo doknotifikasjonStatusTo = new DoknotifikasjonStatusTo(null, BESTILLINGS_ID, OPPRETTET, MELDING, DISTRIBUSJON_ID);

		DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
				doknotifikasjonStatusValidator.validateInput(doknotifikasjonStatusTo));
		assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic aapen-dok-notifikasjon-status har tom verdi for bestillingsId.", exception.getMessage());
	}

	@Test
	public void shouldNotValidateWithoutBestillerId() {
		DoknotifikasjonStatusTo doknotifikasjonStatusTo = new DoknotifikasjonStatusTo(BESTILLER_ID, null, OPPRETTET, MELDING, DISTRIBUSJON_ID);

		DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
				doknotifikasjonStatusValidator.validateInput(doknotifikasjonStatusTo));
		assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic aapen-dok-notifikasjon-status har tom verdi for bestillerId.", exception.getMessage());
	}

	@Test
	public void shouldNotValidateWithoutMelding() {
		DoknotifikasjonStatusTo doknotifikasjonStatusTo = new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLINGS_ID, OPPRETTET, null, DISTRIBUSJON_ID);

		DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
				doknotifikasjonStatusValidator.validateInput(doknotifikasjonStatusTo));
		assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic aapen-dok-notifikasjon-status har tom verdi for melding.", exception.getMessage());
	}

	@Test
	public void shouldNotValidateWithoutStatus() {
		DoknotifikasjonStatusTo doknotifikasjonStatusTo = new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLINGS_ID, null, MELDING, DISTRIBUSJON_ID);

		DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
				doknotifikasjonStatusValidator.validateInput(doknotifikasjonStatusTo));
		assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic aapen-dok-notifikasjon-status har tom verdi for status.", exception.getMessage());
	}
}