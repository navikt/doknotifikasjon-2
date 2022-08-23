package no.nav.doknotifikasjon.consumer;

import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.DigitalKontaktinformasjonTo.DigitalKontaktinfo;
import no.nav.doknotifikasjon.consumer.sikkerhetsnivaa.AuthLevelResponse;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.schemas.PrefererteKanal;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonMap;

public final class TestUtils {

	public static final String BESTILLINGS_ID = "bestillingsId";
	public static final String BESTILLER_ID = "bestillerId";
	public static final String FODSELSNUMMER = "12345678901";
	public static final int SIKKERHETSNIVAA = 0;
	public static final int ANTALL_RENOTIFIKASJONER = 20;
	public static final int RENOTIFIKASJON_INTERVALL = 20;
	public static final String TITTEL = "tittel";
	public static final String EPOST_TEKST = "epostTekst";
	public static final String EPOST_SMS = "smsTekst";
	public static final List<PrefererteKanal> PREFERTE_KANALER = List.of(PrefererteKanal.EPOST, PrefererteKanal.SMS);
	public static final List<PrefererteKanal> PREFERTE_KANALER_WITH_ONLY_SMS = List.of(PrefererteKanal.SMS);
	public static final List<Kanal> PREFERTE_KANALER_TO = List.of(Kanal.EPOST, Kanal.SMS);

	public TestUtils() {
	}

	public static Doknotifikasjon createDoknotifikasjon() {
		return new Doknotifikasjon(
				BESTILLINGS_ID,
				BESTILLER_ID,
				SIKKERHETSNIVAA,
				FODSELSNUMMER,
				ANTALL_RENOTIFIKASJONER,
				RENOTIFIKASJON_INTERVALL,
				TITTEL,
				EPOST_TEKST,
				EPOST_SMS,
				PREFERTE_KANALER
		);
	}

	public static Doknotifikasjon createDoknotifikasjonWithInvalidFnr() {
		return new Doknotifikasjon(
				BESTILLINGS_ID,
				BESTILLER_ID,
				SIKKERHETSNIVAA,
				"123",
				ANTALL_RENOTIFIKASJONER,
				RENOTIFIKASJON_INTERVALL,
				TITTEL,
				EPOST_TEKST,
				EPOST_SMS,
				PREFERTE_KANALER
		);
	}

	public static Doknotifikasjon createDoknotifikasjonWithPreferedKanalAsSms() {
		return new Doknotifikasjon(
				BESTILLINGS_ID,
				BESTILLER_ID,
				SIKKERHETSNIVAA,
				FODSELSNUMMER,
				ANTALL_RENOTIFIKASJONER,
				RENOTIFIKASJON_INTERVALL,
				TITTEL,
				EPOST_TEKST,
				EPOST_SMS,
				PREFERTE_KANALER_WITH_ONLY_SMS
		);
	}

	public static Doknotifikasjon createDoknotifikasjonWithInvalidAntallRenotifikasjoner() {
		return new Doknotifikasjon(
				BESTILLINGS_ID,
				BESTILLER_ID,
				SIKKERHETSNIVAA,
				FODSELSNUMMER,
				ANTALL_RENOTIFIKASJONER,
				0,
				TITTEL,
				EPOST_TEKST,
				EPOST_SMS,
				PREFERTE_KANALER
		);
	}


	public static DoknotifikasjonTO createDoknotifikasjonTO() {
		return new DoknotifikasjonTO(
				BESTILLINGS_ID,
				BESTILLER_ID,
				FODSELSNUMMER,
				ANTALL_RENOTIFIKASJONER,
				RENOTIFIKASJON_INTERVALL,
				TITTEL,
				EPOST_TEKST,
				EPOST_SMS,
				PREFERTE_KANALER_TO,
				SIKKERHETSNIVAA
		);
	}

	public static Doknotifikasjon createDoknotifikasjonWithSikkerhetsnivaa(int sikkerhetsnivaa) {
		return new Doknotifikasjon(
				BESTILLINGS_ID,
				BESTILLER_ID,
				sikkerhetsnivaa,
				FODSELSNUMMER,
				ANTALL_RENOTIFIKASJONER,
				RENOTIFIKASJON_INTERVALL,
				TITTEL,
				EPOST_TEKST,
				EPOST_SMS,
				PREFERTE_KANALER
		);
	}

	public static DigitalKontaktinformasjonTo createDigitalKontaktinformasjonInfo() {
		return new DigitalKontaktinformasjonTo(
				null,
				singletonMap(FODSELSNUMMER, createKontaktInfo("bogus", "bogus", true, false))
		);
	}

	public static DigitalKontaktinformasjonTo createDKIFWithKanVarslesFalse() {
		return new DigitalKontaktinformasjonTo(
				null,
				singletonMap(FODSELSNUMMER, createKontaktInfo("bogus", "bogus", false, false))
		);
	}

	public static DigitalKontaktinformasjonTo createEmptyDigitalKontaktinformasjonInfo() {
		return new DigitalKontaktinformasjonTo(
				null,
				Collections.emptyMap()
		);
	}

	public static DigitalKontaktinformasjonTo createDigitalKontaktinformasjonInfoWithErrorMessage() {
		return new DigitalKontaktinformasjonTo(
				singletonMap(FODSELSNUMMER, "Ingen kontaktinformasjon er registrert på personen"),
				null
		);
	}

	public static DigitalKontaktinformasjonTo createInvalidKontaktInfoWithoutKontaktInfo() {
		return new DigitalKontaktinformasjonTo(
				null,
				singletonMap(FODSELSNUMMER, createKontaktInfo(null, null, true, false))
		);
	}

	public static DigitalKontaktinformasjonTo createValidKontaktInfoReserved() {
		return new DigitalKontaktinformasjonTo(
				null,
				singletonMap(FODSELSNUMMER, createKontaktInfo("bogus", "bogus", true, true))
		);
	}

	public static DigitalKontaktinformasjonTo createInvalidKontaktInfo() {
		return new DigitalKontaktinformasjonTo(
				null,
				singletonMap(FODSELSNUMMER, createKontaktInfo("bogus", "bogus", false, false))
		);
	}

	public static DigitalKontaktinfo createValidKontaktInfo() {
		return createKontaktInfo("bogus", "bogus", true, false);
	}

	public static DigitalKontaktinfo createKontaktInfo(
			String epost,
			String sms,
			boolean varsel,
			boolean reservert
	) {
		return DigitalKontaktinfo.builder()
				.epostadresse(epost)
				.mobiltelefonnummer(sms)
				.kanVarsles(varsel)
				.reservert(reservert)
				.build();
	}

	public static AuthLevelResponse createAuthLevelResponse() {
		return new AuthLevelResponse(true, FODSELSNUMMER);
	}

}
