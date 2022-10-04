package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.MottakerIdType;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;

public class TestUtils {
	public static final String BESTILLINGS_ID = "1234-5678-9101";
	public static final String BESTILLER_ID = "teamdokumenthandtering";
	public static final String MOTTAKER_ID = "03116823216";
	public static final String PREFERERTE_KANALER = Kanal.EPOST.toString();
	public static final String OPPRETTET_AV = "srvdokument";
	public static final String OPPRETTET_AV_2 = "srvdokopp";
	public static final String ENDRET_AV = "srvdokumentlosninger";
	public static final String ENDRET_AV_2 = "srvdokmot";
	public static final String KONTAKTINFO = "Hallohallo";
	public static final String TITTEL = "Melding";
	public static final String TEKST = "Lang tekst";
	public static final Integer ANTALL_RENOTIFIKASJONER = 2;
	public static final Integer RENOTIFIKASJON_INTERVALL = 7;
	public static final LocalDate NESTE_RENOTIFIKASJON_DATO = LocalDate.parse("2020-10-03");
	public static final LocalDateTime OPPRETTET_DATO = LocalDateTime.parse("2020-10-01T10:15:30.000000");
	public static final LocalDateTime OPPRETTET_DATO_2 = LocalDateTime.parse("2020-10-06T10:15:30.000000");
	public static final LocalDateTime SENDT_DATO = LocalDateTime.parse("2020-10-04T10:15:30.000000");
	public static final LocalDateTime ENDRET_DATO = LocalDateTime.parse("2020-10-02T10:15:30.000000");
	public static final LocalDateTime ENDRET_DATO_2 = LocalDateTime.parse("2020-10-05T10:15:30.000000");


	public static Notifikasjon createNotifikasjon() {
		return Notifikasjon.builder()
				.bestillerId(BESTILLER_ID)
				.bestillingsId(BESTILLINGS_ID)
				.mottakerId(MOTTAKER_ID)
				.mottakerIdType(MottakerIdType.FNR)
				.status(OPPRETTET)
				.antallRenotifikasjoner(ANTALL_RENOTIFIKASJONER)
				.renotifikasjonIntervall(RENOTIFIKASJON_INTERVALL)
				.nesteRenotifikasjonDato(NESTE_RENOTIFIKASJON_DATO)
				.prefererteKanaler(PREFERERTE_KANALER)
				.opprettetAv(OPPRETTET_AV)
				.opprettetDato(OPPRETTET_DATO)
				.endretAv(ENDRET_AV)
				.endretDato(ENDRET_DATO)
				.build();
	}

	public static Notifikasjon createNotifikasjonWithId() {
		return Notifikasjon.builder()
				.id(1)
				.bestillerId(BESTILLER_ID)
				.bestillingsId(BESTILLINGS_ID)
				.mottakerId(MOTTAKER_ID)
				.mottakerIdType(MottakerIdType.FNR)
				.status(OPPRETTET)
				.antallRenotifikasjoner(ANTALL_RENOTIFIKASJONER)
				.renotifikasjonIntervall(RENOTIFIKASJON_INTERVALL)
				.nesteRenotifikasjonDato(NESTE_RENOTIFIKASJON_DATO)
				.prefererteKanaler(PREFERERTE_KANALER)
				.opprettetAv(OPPRETTET_AV)
				.opprettetDato(OPPRETTET_DATO)
				.endretAv(ENDRET_AV)
				.endretDato(ENDRET_DATO)
				.build();
	}

	public static NotifikasjonDistribusjon createNotifikasjonDistribusjonWithId(Notifikasjon notifikasjon, Kanal kanal, int id) {
		return NotifikasjonDistribusjon.builder()
				.id(id)
				.notifikasjon(notifikasjon)
				.status(FERDIGSTILT)
				.kanal(kanal)
				.kontaktInfo(KONTAKTINFO)
				.tittel(TITTEL)
				.tekst(TEKST)
				.sendtDato(SENDT_DATO)
				.opprettetAv(OPPRETTET_AV_2)
				.opprettetDato(OPPRETTET_DATO_2)
				.endretAv(ENDRET_AV_2)
				.endretDato(OPPRETTET_DATO)
				.build();
	}

	public static NotifikasjonDistribusjon createNotifikasjonDistribusjon(Notifikasjon notifikasjon, LocalDateTime sistEndret, Kanal kanal) {
		return NotifikasjonDistribusjon.builder()
				.notifikasjon(notifikasjon)
				.status(FERDIGSTILT)
				.kanal(kanal)
				.kontaktInfo(KONTAKTINFO)
				.tittel(TITTEL)
				.tekst(TEKST)
				.sendtDato(SENDT_DATO)
				.opprettetAv(OPPRETTET_AV_2)
				.opprettetDato(OPPRETTET_DATO_2)
				.endretAv(ENDRET_AV_2)
				.endretDato(sistEndret)
				.build();
	}
}
