package no.nav.doknotifikasjon.repository;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {RepositoryConfig.class})
@ActiveProfiles("itest")
public class NotifikasjonRepositoryTest {

	private static final String BESTILLINGS_ID = "1234-5678-9101";
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

		assertEquals(BESTILLINGS_ID, notifikasjon.getBestillingsId());
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
		createNotifikasjonDistribusjonWithNotifikasjon(notifikasjon);
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjon(notifikasjon));

		assertEquals(1L, notifikasjonRepository.count());
		assertEquals(1L, notifikasjonDistribusjonRepository.count());

		NotifikasjonDistribusjon notfikasjonDistribusjon = notifikasjonDistribusjonRepository.findAllByNotifikasjonIn(Collections.singletonList(notifikasjon)).get(0);
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

	@Test
	public void shouldGetNotifikasjonerForSnot001() {
		List<Notifikasjon> notifikasjonList = new ArrayList<>();
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(Status.FEILET, 2, LocalDate.now().minusDays(1)));
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(Status.OVERSENDT, 2, LocalDate.now().minusDays(1)));
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(Status.OVERSENDT, 2, LocalDate.now().plusDays(1)));
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(Status.OVERSENDT, 0, LocalDate.now().minusDays(1)));
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(Status.OVERSENDT, 3, LocalDate.now().minusDays(1)));
		notifikasjonRepository.saveAll(notifikasjonList);

		List<Notifikasjon> notifikasjonsForSnot001 = notifikasjonRepository.findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoBefore(Status.OVERSENDT, 0, LocalDate.now());

		assertEquals(2, notifikasjonsForSnot001.size());
		notifikasjonsForSnot001.forEach(notifikasjon -> {
			assertEquals(Status.OVERSENDT, notifikasjon.getStatus());
			assertTrue(notifikasjon.getAntallRenotifikasjoner() > 0);
			assertTrue(LocalDate.now().isAfter(notifikasjon.getNesteRenotifikasjonDato()));
		});
	}

	@Test
	public void shouldGetNotifikasjonDistribusjonForSnot001() {
		Notifikasjon notifikasjon_1 = createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(Status.OVERSENDT, 3, LocalDate.now().minusDays(1));
		Notifikasjon notifikasjon_2 = createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(Status.OVERSENDT, 3, LocalDate.now().minusDays(1));
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjon(notifikasjon_1));
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjon(notifikasjon_2));

		List<Notifikasjon> notifikasjonList = new ArrayList<>();
		notifikasjonList.add(notifikasjon_1);
		notifikasjonList.add(notifikasjon_2);

		assertEquals(2, notifikasjonDistribusjonRepository.findAllByNotifikasjonIn(notifikasjonList).size());
	}

	private NotifikasjonDistribusjon createNotifikasjonDistribusjonWithNotifikasjon(Notifikasjon notifikasjon) {
		return NotifikasjonDistribusjon.builder()
				.notifikasjon(notifikasjon)
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
				.bestillingsId(BESTILLINGS_ID)
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

	private Notifikasjon createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(Status status, Integer antallRenotifikasjoner, LocalDate nesteRenotifikasjonsDato) {
		return Notifikasjon.builder()
				.bestillerId(BESTILLER_ID)
				.bestillingsId(BESTILLINGS_ID)
				.mottakerId(MOTTAKER_ID)
				.mottakerIdType(MottakerIdType.FNR)
				.status(status)
				.antallRenotifikasjoner(antallRenotifikasjoner)
				.renotifikasjonIntervall(RENOTIFIKASJON_INTERVALL)
				.nesteRenotifikasjonDato(nesteRenotifikasjonsDato)
				.prefererteKanaler(PREFERERTE_KANALER)
				.opprettetAv(OPPRETTET_AV)
				.opprettetDato(OPPRETTET_DATO)
				.endretAv(ENDRET_AV)
				.endretDato(ENDRET_DATO)
				.build();
	}
}
