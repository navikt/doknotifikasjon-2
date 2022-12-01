package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.KontaktinfoTo;
import no.nav.doknotifikasjon.consumer.sikkerhetsnivaa.AuthLevelResponse;

public class TestUtils {

	public static final String FODSELSNUMMER = "12345678901";

	public static KontaktinfoTo createDigitalKontaktinformasjonInfo(
			boolean varsel,
			boolean reservert,
			String epost,
			String sms
	) {
		return createKontaktinfoTo(varsel, reservert, epost, sms);
	}

	public static KontaktinfoTo createKontaktinfoTo(
			boolean varsel,
			boolean reservert,
			String epost,
			String sms
	) {
		return new KontaktinfoTo(varsel, reservert, epost, sms);
	}

	public static AuthLevelResponse createAuthLevelResponse(boolean harBruktSikkerhetsnivaa4) {
		return new AuthLevelResponse(harBruktSikkerhetsnivaa4, FODSELSNUMMER);
	}

}
