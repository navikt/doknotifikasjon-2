package no.nav.doknotifikasjon.domain;

import no.nav.doknotifikasjon.exception.functional.UgyldigMobiltelefonnummerException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MobiltelefonnummerTest {

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
	void shouldValidateMobiltelefonnummer(String gyldigMobiltelefonnummer) {
		var result = new Mobiltelefonnummer(gyldigMobiltelefonnummer);

		assertEquals(gyldigMobiltelefonnummer, result.mobiltelefonnummer());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"12345678",
			"4234567",
			"+46212121212121212121",
			"004621212121212121212",
			"12345678a",
			"1234+1234",
			"1234_1234"
	})
	void shouldNotValidateMobiltelefonnummer(String ugyldigMobiltelefonnummer) {
		assertThrows(UgyldigMobiltelefonnummerException.class, () -> new Mobiltelefonnummer(ugyldigMobiltelefonnummer));
	}

}
