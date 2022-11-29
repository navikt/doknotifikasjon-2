package no.nav.doknotifikasjon.domain;

import no.nav.doknotifikasjon.exception.functional.UgyldigEpostException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EpostadresseTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"andersANDERS@gmail.com",
			"0123@gmail.com",
			"!#$%&'*+-/=?^_`{|}~@gmail.com",
			"_denne_delen_av_adressen_har_noeyaktig_femti_tegn_@gmail.com",
	})
	void shouldValidateEpost(String gyldigEpost) {
		var result = new Epostadresse(gyldigEpost);

		assertEquals(gyldigEpost, result.epostadresse());
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
		assertThrows(UgyldigEpostException.class, () -> new Epostadresse(ugyldigEpost));
	}
}
