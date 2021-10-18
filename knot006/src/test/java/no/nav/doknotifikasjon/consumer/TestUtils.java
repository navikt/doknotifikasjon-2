package no.nav.doknotifikasjon.consumer;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo;
import no.nav.doknotifikasjon.schemas.PrefererteKanal;

import java.util.List;

public final class TestUtils {

	public static final String BESTILLINGS_ID = "bestillingsId";
	public static final String BESTILLER_ID = "bestillerId";
	public static final String FODSELSNUMMER = "12345678901";
	public static final int ANTALL_RENOTIFIKASJONER = 20;
	public static final int RENOTIFIKASJON_INTERVALL = 20;
	public static final String TITTEL = "tittel";
	public static final String EPOST_TEKST = "epostTekst";
	public static final String EPOST_SMS = "smsTekst";

	public static final String MOBILTELEFONNUMMER = "9999999";
	public static final String EPOSTADRESSE = "epost@epost";

	public static final List<PrefererteKanal> PREFERTE_KANALER = List.of(PrefererteKanal.EPOST, PrefererteKanal.SMS);
	public static final List<PrefererteKanal> PREFERTE_KANALER_WITH_ONLY_SMS = List.of(PrefererteKanal.SMS);
	public static final List<PrefererteKanal> PREFERTE_KANALER_WITH_ONLY_EPOST = List.of(PrefererteKanal.EPOST);
	public static final List<Kanal> PREFERTE_KANALER_TO = List.of(Kanal.EPOST, Kanal.SMS);

	public static NotifikasjonMedkontaktInfo createNotifikasjon() {
		return createDoknotifikasjon(PREFERTE_KANALER, MOBILTELEFONNUMMER, EPOSTADRESSE);
	}

	public static NotifikasjonMedkontaktInfo createDoknotifikasjonWithPreferedKanalAndEpostIsnull() {
		return createDoknotifikasjon(PREFERTE_KANALER_WITH_ONLY_EPOST, MOBILTELEFONNUMMER, null);
	}

	public static NotifikasjonMedkontaktInfo createDoknotifikasjonWithPreferedKanalAsEpost() {
		return createDoknotifikasjon(PREFERTE_KANALER_WITH_ONLY_EPOST, MOBILTELEFONNUMMER, EPOSTADRESSE);
	}

	public static NotifikasjonMedkontaktInfo createDoknotifikasjonWithPreferedKanalAsSms() {
		return createDoknotifikasjon(PREFERTE_KANALER_WITH_ONLY_SMS, MOBILTELEFONNUMMER, EPOSTADRESSE);
	}

	public static NotifikasjonMedkontaktInfo createDoknotifikasjonWithoutEpostOrSms() {
		return createDoknotifikasjon(PREFERTE_KANALER_WITH_ONLY_SMS, null, null);
	}

	public static NotifikasjonMedkontaktInfo createDoknotifikasjon(
			List<PrefererteKanal> prefererteKanalList,
			String mobiltelefonnummer,
			String epostadresse
	) {
		return new NotifikasjonMedkontaktInfo(
				BESTILLINGS_ID,
				BESTILLER_ID,
				FODSELSNUMMER,
				mobiltelefonnummer,
				epostadresse,
				ANTALL_RENOTIFIKASJONER,
				RENOTIFIKASJON_INTERVALL,
				TITTEL,
				EPOST_TEKST,
				EPOST_SMS,
				prefererteKanalList
		);
	}

	public static NotifikasjonMedkontaktInfo createDoknotifikasjonWithInvalidAntallRenotifikasjoner() {
		return new NotifikasjonMedkontaktInfo(
				BESTILLINGS_ID,
				BESTILLER_ID,
				FODSELSNUMMER,
				MOBILTELEFONNUMMER,
				EPOSTADRESSE,
				ANTALL_RENOTIFIKASJONER,
				0,
				TITTEL,
				EPOST_TEKST,
				EPOST_SMS,
				PREFERTE_KANALER
		);
	}

	public static NotifikasjonMedKontaktInfoTO createDoknotifikasjonTO() {
		return new NotifikasjonMedKontaktInfoTO(
				BESTILLINGS_ID,
				BESTILLER_ID,
				FODSELSNUMMER,
				MOBILTELEFONNUMMER,
				EPOSTADRESSE,
				ANTALL_RENOTIFIKASJONER,
				RENOTIFIKASJON_INTERVALL,
				TITTEL,
				EPOST_TEKST,
				EPOST_SMS,
				PREFERTE_KANALER_TO
		);
	}
}
