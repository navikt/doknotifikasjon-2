package no.nav.doknotifikasjon.utils;

import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;

import java.time.LocalDateTime;
import java.util.Collections;

public final class TestUtils {

	public TestUtils() {
	}

	public static final String BESTILLING_ID = "1234-5678-9101";
	public static final String BESTILLER_ID = "teamdokumenthandtering";
	public static final String BESTILLER_ID_2 = "teamsaf";
	public static final String STATUS = Status.OPPRETTET.toString();
	public static final String UGYLDIG_STATUS = "OPRETET";
	public static final String MELDING = "Heiheihei";
	public static final Long DISTRIBUSJON_ID = 987654321L;
	private static final LocalDateTime OPPRETTET_DATO = LocalDateTime.parse("2020-10-01T10:15:30.000000");


	public static Notifikasjon createNotifikasjon(){
		return Notifikasjon.builder()
				.bestillingId(BESTILLING_ID)
				.bestillerId(BESTILLER_ID)
				.status(Status.FEILET)
				.notifikasjonDistribusjon(Collections.emptySet())
				.opprettetDato(OPPRETTET_DATO)
				.build();
	}
}
