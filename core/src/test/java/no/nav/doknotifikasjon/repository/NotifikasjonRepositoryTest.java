package no.nav.doknotifikasjon.repository;

import static org.junit.Assert.assertEquals;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.MottakerIdType;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {RepositoryConfig.class})
@ActiveProfiles("itest")
public class NotifikasjonRepositoryTest {

	private static final String BESTILLING_ID = "1234-5678-9101";
	private static final String BESTILLER_ID = "teamdokumenthandtering";
	private static final String MOTTAKER_ID = "03116823216";
	private static final String PREFERERTE_KANALER = Kanal.EPOST.toString();
	private static final String OPPRETTET_AV = "srvdokument";
	private static final String OPPRETTET_AV_2 = "srvdokopp";
	private static final String ENDRET_AV = "srvdokumentlosninger";
	private static final String ENDRET_AV_2 = "srvdokmot";
	private static final String KONTAKTINFO = "Hallohallo";
	private static final String TITTEL = "Melding";
	private static final String TEKST = "Lang tekst";
	private static final Integer ANTALL_RENOTIFIKASJONER = 2;
	private static final Integer RENOTIFIKASJON_INTERVALL = 7;
	private static final LocalDate NESTE_RENOTIFIKASJON_DATO = LocalDate.parse("2020-10-03");
	private static final LocalDateTime OPPRETTET_DATO = LocalDateTime.parse("2020-10-01T10:15:30.000000");
	private static final LocalDateTime OPPRETTET_DATO_2 = LocalDateTime.parse("2020-10-06T10:15:30.000000");
	private static final LocalDateTime SENDT_DATO = LocalDateTime.parse("2020-10-04T10:15:30.000000");
	private static final LocalDateTime ENDRET_DATO = LocalDateTime.parse("2020-10-02T10:15:30.000000");
	private static final LocalDateTime ENDRET_DATO_2 = LocalDateTime.parse("2020-10-05T10:15:30.000000");


	@Autowired
	private NotifikasjonRepository notifikasjonRepository;

	@Autowired
	private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@BeforeEach
	public void setup() {
		notifikasjonRepository.deleteAll();
		notifikasjonDistribusjonRepository.deleteAll();
	}

	@Test
	public void shouldOpprettNotifikasjon() {
		assertEquals(0L, notifikasjonRepository.count());
		notifikasjonRepository.saveAndFlush(createNotifikasjon());

		assertEquals(1L, notifikasjonRepository.count());

		Notifikasjon notifikasjon = notifikasjonRepository.findById(1).get();

		assertEquals(BESTILLING_ID, notifikasjon.getBestillingId());
		assertEquals(BESTILLER_ID, notifikasjon.getBestillerId());
		assertEquals(MOTTAKER_ID, notifikasjon.getMottakerId());
		assertEquals(PREFERERTE_KANALER, notifikasjon.getPrefererteKanaler());
		assertEquals(OPPRETTET_AV, notifikasjon.getOpprettetAv());
		assertEquals(ANTALL_RENOTIFIKASJONER, notifikasjon.getAntallRenotifikasjoner());
		assertEquals(RENOTIFIKASJON_INTERVALL, notifikasjon.getRenotifikasjonIntervall());
		assertEquals(NESTE_RENOTIFIKASJON_DATO, notifikasjon.getNesteRenotifikasjonDato());
		assertEquals(OPPRETTET_DATO, notifikasjon.getOpprettetDato());
		assertEquals(ENDRET_DATO, notifikasjon.getEndretDato());
		assertEquals(Status.OPPRETTET, notifikasjon.getStatus());
		assertEquals(MottakerIdType.FNR, notifikasjon.getMottakerIdType());
	}

	@Test
	public void shouldOpprettNotifikasjonDistribusjon() {
		assertEquals(0L, notifikasjonDistribusjonRepository.count());
		assertEquals(0L, notifikasjonRepository.count());

		Notifikasjon notifikasjon = createNotifikasjon();
		createNotifikasjonDistribusjonWithNotifikasjonId(notifikasjon);
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonId(notifikasjon));

		assertEquals(1L, notifikasjonRepository.count());
		assertEquals(1L, notifikasjonDistribusjonRepository.count());

		NotifikasjonDistribusjon notfikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(1).get();
		assertEquals(Status.FERDIGSTILT, notfikasjonDistribusjon.getStatus());
		assertEquals(Kanal.SMS, notfikasjonDistribusjon.getKanal());
		assertEquals(KONTAKTINFO, notfikasjonDistribusjon.getKontaktInfo());
		assertEquals(TITTEL, notfikasjonDistribusjon.getTittel());
		assertEquals(TEKST, notfikasjonDistribusjon.getTekst());
		assertEquals(SENDT_DATO, notfikasjonDistribusjon.getSendtDato());
		assertEquals(OPPRETTET_AV_2, notfikasjonDistribusjon.getOpprettetAv());
		assertEquals(OPPRETTET_DATO_2, notfikasjonDistribusjon.getOpprettetDato());
		assertEquals(ENDRET_AV_2, notfikasjonDistribusjon.getEndretAv());
		assertEquals(ENDRET_DATO_2, notfikasjonDistribusjon.getEndretDato());
	}

	private NotifikasjonDistribusjon createNotifikasjonDistribusjonWithNotifikasjonId(Notifikasjon notifikasjon) {
		return NotifikasjonDistribusjon.builder()
				.notifikasjonId(notifikasjon)
				.status(Status.FERDIGSTILT)
				.kanal(Kanal.SMS)
				.kontaktInfo(KONTAKTINFO)
				.tittel(TITTEL)
				.tekst(TEKST)
				.sendtDato(SENDT_DATO)
				.opprettetAv(OPPRETTET_AV_2)
				.opprettetDato(OPPRETTET_DATO_2)
				.endretAv(ENDRET_AV_2)
				.endretDato(ENDRET_DATO_2)
				.build();
	}

	private Notifikasjon createNotifikasjon() {
		return Notifikasjon.builder()
				.bestillerId(BESTILLER_ID)
				.bestillingId(BESTILLING_ID)
				.mottakerId(MOTTAKER_ID)
				.mottakerIdType(MottakerIdType.FNR)
				.status(Status.OPPRETTET)
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
}
