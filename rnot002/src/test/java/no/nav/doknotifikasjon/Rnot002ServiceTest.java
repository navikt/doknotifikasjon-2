package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.exception.functional.DigitalKontaktinformasjonFunctionalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static no.nav.doknotifikasjon.TestUtils.createDigitalKontaktinformasjonInfo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Rnot002ServiceTest {

	private static final String PERSONIDENT = "12345678901";
	private static final String EPOST = "epost@nav.no";
	private static final String SMS = "91234567";

	@Mock
	DigitalKontaktinfoConsumer digitalKontaktinfoConsumer;

	@InjectMocks
	Rnot002Service rnot002Service;

	@Test
	void kanVarsles() {
		when(digitalKontaktinfoConsumer.hentDigitalKontaktinfoForPerson(PERSONIDENT))
				.thenReturn(createDigitalKontaktinformasjonInfo(true, false, EPOST, SMS));

		var result = rnot002Service.getKanVarsles(PERSONIDENT);

		assertTrue(result.kanVarsles());
	}

	@ParameterizedTest
	@MethodSource
	void kanIkkeVarsles(String epost,
						String sms,
						boolean kanVarsles,
						boolean reservert) {

		when(digitalKontaktinfoConsumer.hentDigitalKontaktinfoForPerson(PERSONIDENT))
				.thenReturn(createDigitalKontaktinformasjonInfo(kanVarsles, reservert, epost, sms));

		var result = rnot002Service.getKanVarsles(PERSONIDENT);

		assertFalse(result.kanVarsles());
	}

	public static Stream<Arguments> kanIkkeVarsles() {
		return Stream.of(
				Arguments.of(EPOST, SMS, false, false),
				Arguments.of(EPOST, SMS, false, true),
				Arguments.of(EPOST, SMS, true, true),
				Arguments.of("", "", true, false)
		);
	}

	// 403 mangler tilgang til person (kode 6/7 etc.)
	// 404 person finnes ikke
	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {"FORBIDDEN", "NOT_FOUND"})
	void kanIkkeVarsles403Eller404FraDigdir(HttpStatus httpStatus) {
		when(digitalKontaktinfoConsumer.hentDigitalKontaktinfoForPerson(PERSONIDENT))
				.thenThrow(new DigitalKontaktinformasjonFunctionalException("message", null, httpStatus));

		var result = rnot002Service.getKanVarsles(PERSONIDENT);

		assertFalse(result.kanVarsles());
	}

	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {"UNAUTHORIZED", "BAD_REQUEST"})
	void kanIkkeVarslesNaar4xxFraDigdir(HttpStatus httpStatus) {
		when(digitalKontaktinfoConsumer.hentDigitalKontaktinfoForPerson(PERSONIDENT))
				.thenThrow(new DigitalKontaktinformasjonFunctionalException("message", null, httpStatus));

		assertThrows(DigitalKontaktinformasjonFunctionalException.class, () -> rnot002Service.getKanVarsles(PERSONIDENT));
	}
}