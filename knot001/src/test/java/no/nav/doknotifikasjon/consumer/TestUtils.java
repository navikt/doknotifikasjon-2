package no.nav.doknotifikasjon.consumer;

import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.schemas.PrefererteKanal;

import java.util.Collections;
import java.util.List;

public final class TestUtils {

    public TestUtils() {}

    public static String BESTILLINGS_ID = "bestillingsId";
    public static String BESTILLER_ID = "bestillerId";
    public static String FODSELSNUMMER = "123456789012341";
    public static String FODSELSNUMMER_FOR_KONTAKT_INFO_WITH_ONLY_EPOST = "123456789012341";
    public static int ANTALL_RENOTIFIKASJONER = 20;
    public static int RENOTIFIKASJON_INTERVALL = 100;
    public static String TITTEL = "tittel";
    public static String EPOST_TEKST = "epostTekst";
    public static String EPOST_SMS = "smsTekst";
    public static List<PrefererteKanal> PREFERTE_KANALER = List.of(PrefererteKanal.EPOST, PrefererteKanal.SMS);
    public static List<PrefererteKanal> PREFERTE_KANALER_WITH_ONLY_SMS = List.of(PrefererteKanal.SMS);
    public static List<Kanal> PREFERTE_KANALER_TO = List.of(Kanal.EPOST, Kanal.SMS);


    public static Doknotifikasjon createDoknotifikasjon() {
        return new Doknotifikasjon(
                BESTILLINGS_ID,
                BESTILLER_ID,
                FODSELSNUMMER,
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
                PREFERTE_KANALER_TO
        );
    }

    public static DigitalKontaktinformasjonTo createDigitalKontaktinformasjonInfo() {
        return new DigitalKontaktinformasjonTo(
                null,
                Collections.singletonMap(FODSELSNUMMER, createKontaktInfo("bogus", "bogus", true, false))
        );
    }

    public static DigitalKontaktinformasjonTo createEmptyDigitalKontaktinformasjonInfo() {
        return new DigitalKontaktinformasjonTo(
                null,
                Collections.emptyMap()
        );
    }

    public static DigitalKontaktinformasjonTo createDigitalKontaktinformasjonInfoWithErrorMessage() {
        DigitalKontaktinformasjonTo.Melding melding = new DigitalKontaktinformasjonTo.Melding();
        melding.setMelding("Ingen kontaktinformasjon er registrert på personen");

        return new DigitalKontaktinformasjonTo(
                Collections.singletonMap(FODSELSNUMMER, melding),
                null
        );
    }

    public static DigitalKontaktinformasjonTo createInvalidKontaktInfoWithoutKontaktInfo() {
        return new DigitalKontaktinformasjonTo(
                null,
                Collections.singletonMap(FODSELSNUMMER, createKontaktInfo(null, null, true, false))
        );
    }

    public static DigitalKontaktinformasjonTo createValidKontaktInfoReserved() {
        return new DigitalKontaktinformasjonTo(
                null,
                Collections.singletonMap(FODSELSNUMMER, createKontaktInfo("bogus", "bogus", true, true))
        );
    }

    public static DigitalKontaktinformasjonTo createInvalidKontaktInfo() {
        return new DigitalKontaktinformasjonTo(
                null,
                Collections.singletonMap(FODSELSNUMMER, createKontaktInfo("bogus", "bogus", false, false))
        );
    }

    public static DigitalKontaktinformasjonTo.DigitalKontaktinfo createValidKontaktInfo() {
        return createKontaktInfo("bogus", "bogus", true, false);
    }

    public static DigitalKontaktinformasjonTo.DigitalKontaktinfo createKontaktInfo(
            String epost,
            String sms,
            boolean varsel,
            boolean reservert
    ) {
        return DigitalKontaktinformasjonTo.DigitalKontaktinfo.builder()
                .epostadresse(epost)
                .mobiltelefonnummer(sms)
                .kanVarsles(varsel)
                .reservert(reservert)
                .build();
    }

}
