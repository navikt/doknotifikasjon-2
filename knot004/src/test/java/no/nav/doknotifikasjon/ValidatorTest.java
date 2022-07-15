package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_FUNCTIONAL_EXCEPTION_DIGDIR_KRR_PROXY;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_FUNCTIONAL_EXCEPTION_SIKKERHETSNIVAA;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SIKKERHETSNIVAA;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_TECHNICAL_EXCEPTION_DATABASE;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT;
import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;
import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLER_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLINGS_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.DISTRIBUSJON_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.MELDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidatorTest {

	private final Validator validator = new Validator();
	private static final String FEILET_FEIL_SOM_SKJER_ETTER_LAGRING_AV_NOTIFIKASJON_I_DB = "Feil etter lagring i db";

	@Test
	public void shouldValidateInput() {
		validator.validateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLINGS_ID, OPPRETTET, MELDING, DISTRIBUSJON_ID));
	}

	@Test
	public void shouldValidateWithoutDistribusjonIdSet() {
		validator.validateInput(new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLINGS_ID, OPPRETTET, MELDING, null));
	}

	@Test
	public void shouldNotValidateWithoutBestillingsId() {
		DoknotifikasjonStatusTo doknotifikasjonStatusTo = new DoknotifikasjonStatusTo(null, BESTILLINGS_ID, OPPRETTET, MELDING, DISTRIBUSJON_ID);

		DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
				validator.validateInput(doknotifikasjonStatusTo));
		assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic teamdokumenthandtering.aapen-dok-notifikasjon-status har tom verdi for bestillingsId.", exception.getMessage());
	}

	@Test
	public void shouldNotValidateWithoutBestillerId() {
		DoknotifikasjonStatusTo doknotifikasjonStatusTo = new DoknotifikasjonStatusTo(BESTILLER_ID, null, OPPRETTET, MELDING, DISTRIBUSJON_ID);

		DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
				validator.validateInput(doknotifikasjonStatusTo));
		assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic teamdokumenthandtering.aapen-dok-notifikasjon-status har tom verdi for bestillerId.", exception.getMessage());
	}

	@Test
	public void shouldNotValidateWithoutMelding() {
		DoknotifikasjonStatusTo doknotifikasjonStatusTo = new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLINGS_ID, OPPRETTET, null, DISTRIBUSJON_ID);

		DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
				validator.validateInput(doknotifikasjonStatusTo));
		assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic teamdokumenthandtering.aapen-dok-notifikasjon-status har tom verdi for melding.", exception.getMessage());
	}

	@Test
	public void shouldNotValidateWithoutStatus() {
		DoknotifikasjonStatusTo doknotifikasjonStatusTo = new DoknotifikasjonStatusTo(BESTILLER_ID, BESTILLINGS_ID, null, MELDING, DISTRIBUSJON_ID);

		DoknotifikasjonValidationException exception = assertThrows(DoknotifikasjonValidationException.class, () ->
				validator.validateInput(doknotifikasjonStatusTo));
		assertEquals("Valideringsfeil i knot004: Hendelse på kafka-topic teamdokumenthandtering.aapen-dok-notifikasjon-status har tom verdi for status.", exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("hentFeilmeldinger")
	public void shouldAvslutteProsesseringForNoenTyperFeilmeldinger(String feilmelding, boolean skalAvslutteProsessering) {
		DoknotifikasjonStatusTo doknotifikasjonStatusTo = new DoknotifikasjonStatusTo.DoknotifikasjonStatusToBuilder()
				.bestillerId(BESTILLER_ID)
				.bestillingsId(BESTILLINGS_ID)
				.status(FEILET)
				.melding(feilmelding)
				.distribusjonId(null)
				.build();

		assertEquals(skalAvslutteProsessering, validator.erStatusInfoEllerFeiletMedSpesiellFeilmelding(doknotifikasjonStatusTo.getStatus(), doknotifikasjonStatusTo.getMelding()));
	}

	private static Stream<Arguments> hentFeilmeldinger() {
		return Stream.of(
				Arguments.of(FEILET_SIKKERHETSNIVAA, true),
				Arguments.of(FEILET_TECHNICAL_EXCEPTION_DATABASE, true),
				Arguments.of(FEILET_FUNCTIONAL_EXCEPTION_SIKKERHETSNIVAA, true),
				Arguments.of(FEILET_FUNCTIONAL_EXCEPTION_DIGDIR_KRR_PROXY, true),
				Arguments.of(FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET, true),
				Arguments.of(FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT, true),
				Arguments.of(FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION, true),
				Arguments.of(FEILET_FEIL_SOM_SKJER_ETTER_LAGRING_AV_NOTIFIKASJON_I_DB, false)
		);
	}
}