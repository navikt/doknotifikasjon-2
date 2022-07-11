package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

public final class TestUtils {

	static final String BESTILLINGS_ID = "1234-5678-9101";
	static final String BESTILLER_ID = "teamdokumenthandtering";
	static final String KONTAKTINFO = "Hallohallo";
	static final String TITTEL = "Melding";
	static final String TEKST = "Lang tekst";
	static final LocalDate NESTE_RENOTIFIKASJONS_DATO = LocalDate.parse("2020-10-01");
	private static final String OPPRETTET_AV = "srvdokument";
	private static final int RENOTIFIKASJON_INTERVALL = 10;
	private static final LocalDateTime SENDT_DATO = LocalDateTime.parse("2020-10-04T10:15:30.000000");
	private static final LocalDateTime OPPRETTET_DATO = LocalDateTime.parse("2020-10-01T10:15:30.000000");
	private static final String PREFERERTE_KANALER = Kanal.EPOST.toString() + ", " + Kanal.SMS.toString();

	public TestUtils() {
	}

	public static Notifikasjon createNotifikasjonWithStatusOversendt(LocalDateTime endretDato) {
		return createNotifikasjonWithStatusOversendtAndAntallRenotifikasjoner(endretDato, 0);
	}

	public static Notifikasjon createNotifikasjonWithStatusOversendtAndAntallRenotifikasjoner(LocalDateTime endretDato, int antallRenotifikasjoner) {
		return Notifikasjon.builder()
				.bestillingsId(BESTILLINGS_ID)
				.bestillerId(BESTILLER_ID)
				.status(Status.OVERSENDT)
				.notifikasjonDistribusjon(Collections.emptySet())
				.antallRenotifikasjoner(antallRenotifikasjoner)
				.nesteRenotifikasjonDato(NESTE_RENOTIFIKASJONS_DATO)
				.renotifikasjonIntervall(RENOTIFIKASJON_INTERVALL)
				.endretDato(endretDato)
				.build();
	}

	public static Notifikasjon createNotifikasjonWithStatus(LocalDateTime endretDato, Status status) {
		return Notifikasjon.builder()
				.bestillingsId(BESTILLINGS_ID)
				.bestillerId(BESTILLER_ID)
				.status(status)
				.notifikasjonDistribusjon(Collections.emptySet())
				.antallRenotifikasjoner(null)
				.nesteRenotifikasjonDato(null)
				.endretDato(endretDato)
				.prefererteKanaler(PREFERERTE_KANALER)
				.opprettetDato(LocalDateTime.now())
				.build();
	}

	public static NotifikasjonDistribusjon createNotifikasjonDistribusjonWithNotifikasjonSetAsFerdigstilt(Notifikasjon notifikasjon, Kanal kanal) {
		return createNotifikasjonDistribusjonWithNotifikasjonAndStatusAndKanal(notifikasjon, Status.FERDIGSTILT, kanal);
	}

	public static NotifikasjonDistribusjon createNotifikasjonDistribusjonWithNotifikasjonAndStatusAndKanal(Notifikasjon notifikasjon, Status status, Kanal kanal) {
		return NotifikasjonDistribusjon.builder()
				.notifikasjon(notifikasjon)
				.status(status)
				.kanal(kanal)
				.kontaktInfo(KONTAKTINFO)
				.tittel(TITTEL)
				.tekst(TEKST)
				.sendtDato(SENDT_DATO)
				.opprettetAv(OPPRETTET_AV)
				.endretDato(LocalDateTime.now().minusDays(29))
				.opprettetDato(OPPRETTET_DATO)
				.build();
	}

}
