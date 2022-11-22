package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.DigitalKontaktinformasjonTo.DigitalKontaktinfo;
import no.nav.doknotifikasjon.consumer.sikkerhetsnivaa.AuthLevelResponse;

import static java.util.Collections.singletonMap;

public class TestUtils {

	public static final String FODSELSNUMMER = "12345678901";

 	public static DigitalKontaktinformasjonTo createDigitalKontaktinformasjonInfo() {
		return new DigitalKontaktinformasjonTo(
				null,
				singletonMap(FODSELSNUMMER, createKontaktInfo("bogus", "bogus", true, false))
		);
	}

	public static DigitalKontaktinformasjonTo createDigitalKontaktinformasjonInfo(
			String epost,
			String sms,
			boolean varsel,
			boolean reservert

	) {
		return new DigitalKontaktinformasjonTo(
				null,
				singletonMap(FODSELSNUMMER, createKontaktInfo(epost, sms, varsel, reservert))
		);
	}

	public static DigitalKontaktinformasjonTo createDigitalKontaktinformasjonInfoMedFeil() {
		return new DigitalKontaktinformasjonTo(
				singletonMap(FODSELSNUMMER, "person_ikke_funnet"),
				null
		);
	}

	public static DigitalKontaktinfo createKontaktInfo(
			String epost,
			String sms,
			boolean varsel,
			boolean reservert
	) {
		return new DigitalKontaktinfo(epost, varsel, sms, reservert);
	}

	public static AuthLevelResponse createAuthLevelResponse(boolean harBruktSikkerhetsnivaa4) {
		return new AuthLevelResponse(harBruktSikkerhetsnivaa4, FODSELSNUMMER);
	}

}
