package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

public final class TestUtils {

	protected static final String BESTILLINGS_ID = "1234-5678-9101";
	protected static final String BESTILLER_ID = "teamdokumenthandtering";
	protected static final String KONTAKTINFO = "Hallohallo";
	protected static final String TITTEL = "Melding";
	protected static final String SNOT001 = "SNOT001";
	protected static final String TEKST = "Lang tekst";
	protected static final String PAAMINNELSE_TEKST = "Påminnelse: " + TEKST;
	protected static final int ANTALL_RENOTIFIKASJONER = 3;
	protected static final LocalDate NESTE_RENOTIFIKASJONS_DATO = LocalDate.parse("2020-10-01");
	private static final String OPPRETTET_AV = "srvdokument";
	private static final int RENOTIFIKASJON_INTERVALL = 10;
	private static final LocalDateTime SENDT_DATO = LocalDateTime.parse("2020-10-04T10:15:30.000000");
	private static final LocalDateTime OPPRETTET_DATO = LocalDateTime.parse("2020-10-01T10:15:30.000000");
	private static final Status DEFAULT_STATUS = Status.OVERSENDT;
	private static final int DEFAULT_ANTALL_RENOTIFIKASJONER = ANTALL_RENOTIFIKASJONER;
	public TestUtils() {
	}

	public static Notifikasjon createNotifikasjonWithAntallRenotifikasjoner(int antallRenotifikasjoner) {
		return createNotifikasjon(antallRenotifikasjoner, DEFAULT_STATUS);
	}


	public static Notifikasjon createDefaultNotifikasjon() {
		return createNotifikasjon(DEFAULT_ANTALL_RENOTIFIKASJONER, DEFAULT_STATUS);
	}

	public static Notifikasjon createNotifikasjonWithStatus(Status status) {
		return createNotifikasjon(DEFAULT_ANTALL_RENOTIFIKASJONER, status);
	}


	private static Notifikasjon createNotifikasjon(int antallRenotifikasjoner, Status status) {
		return Notifikasjon.builder()
				.bestillingsId(BESTILLINGS_ID)
				.bestillerId(BESTILLER_ID)
				.status(status)
				.notifikasjonDistribusjon(Collections.emptySet())
				.antallRenotifikasjoner(antallRenotifikasjoner)
				.nesteRenotifikasjonDato(NESTE_RENOTIFIKASJONS_DATO)
				.renotifikasjonIntervall(RENOTIFIKASJON_INTERVALL)
				.build();
	}

	public static NotifikasjonDistribusjon createNotifikasjonDistribusjonWithNotifikasjonAndKanal(Notifikasjon notifikasjon, Kanal kanal) {
		return NotifikasjonDistribusjon.builder()
				.notifikasjon(notifikasjon)
				.status(Status.OVERSENDT)
				.kanal(kanal)
				.kontaktInfo(KONTAKTINFO)
				.tittel(TITTEL)
				.tekst(TEKST)
				.sendtDato(SENDT_DATO)
				.opprettetAv(OPPRETTET_AV)
				.opprettetDato(OPPRETTET_DATO)
				.build();
	}

}
