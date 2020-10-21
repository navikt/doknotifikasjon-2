package no.nav.doknotifikasjon.utils;

import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

public final class TestUtils {

    public TestUtils() {
    }

    public static final String BESTILLINGS_ID = "1234-5678-9101";
    public static final String BESTILLER_ID = "teamdokumenthandtering";
    public static final String BESTILLER_ID_2 = "teamsaf";
    public static final int ANTALL_RENOTIFIKASJONER = 3;
    private static final LocalDateTime OPPRETTET_DATO = LocalDateTime.parse("2020-10-01T10:15:30.000000");
    public static final LocalDate NESTE_RENOTIFIKASJONSDATO = LocalDate.parse("2020-10-01");

    public static Notifikasjon createNotifikasjonWithStatus(Status status) {
        return Notifikasjon.builder()
                .bestillingsId(BESTILLINGS_ID)
                .bestillerId(BESTILLER_ID)
                .status(status)
                .notifikasjonDistribusjon(Collections.emptySet())
                .opprettetDato(OPPRETTET_DATO)
                .antallRenotifikasjoner(ANTALL_RENOTIFIKASJONER)
                .nesteRenotifikasjonDato(NESTE_RENOTIFIKASJONSDATO)
                .build();
    }
}
