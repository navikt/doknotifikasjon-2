package no.nav.doknotifikasjon;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static no.nav.doknotifikasjon.KontaktinfoValidator.isEpostValid;
import static no.nav.doknotifikasjon.KontaktinfoValidator.isMobilnummerValid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KontaktinfoValidatorTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"andersANDERS@gmail.com",
			"0123@gmail.com",
			"!#$%&'*+-/=?^_`{|}~@gmail.com",
			"_denne_delen_av_adressen_har_noeyaktig_femti_tegn_@gmail.com",
	})
	void shouldValidateEpost(String gyldigEpost) {
		assertTrue(isEpostValid(gyldigEpost));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"@gmail.com",
			"0123@",
			"ingenalfakroellgmail.com",
			".punktumpaastarten@gmail.com",
			"punktumpaaslutten.@gmail.com",
			"to..paarad@example.com",
			"denne_delen_av_adressen_har_noeyaktig_femtien_tegn_@gmail.com",
	})
	void shouldNotValidateEpost(String ugyldigEpost) {
		assertFalse(isEpostValid(ugyldigEpost));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"004742345678",
			"004792345678",
			"+4742345678",
			"+4792345678",
			"42345678",
			"92345678",
			"004620202020202020",
			"+46202020202020202",
			"00461-2-3-4-5"
	})
	void shouldValidateMobiltelefonnummer(String mobiltelefonnummer) {
		assertTrue(isMobilnummerValid(mobiltelefonnummer));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"12345678",
			"4234567",
			//"+462121212121212121212",
			"12345678a",
			"1234+1234",
			"1234_1234"
	})
	void shouldNotValidateMobiltelefonnummer(String mobiltelefonnummer) {
		assertFalse(isMobilnummerValid(mobiltelefonnummer));
	}
}