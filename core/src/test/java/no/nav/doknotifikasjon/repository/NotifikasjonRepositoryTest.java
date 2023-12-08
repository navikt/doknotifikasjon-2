package no.nav.doknotifikasjon.repository;

import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalEC2;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.MottakerIdType;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.time.LocalDate.now;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;
import static no.nav.doknotifikasjon.kodeverk.Status.OVERSENDT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
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

	@MockBean
	INotificationAgencyExternalEC2 iNotificationAgencyExternalEC2;
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
		assertEquals(OPPRETTET, notifikasjon.getStatus());
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
		assertEquals(FERDIGSTILT, notfikasjonDistribusjon.getStatus());
		assertEquals(SMS, notfikasjonDistribusjon.getKanal());
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
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(FEILET, 2, LocalDate.now().minusDays(1), LocalDateTime.now()));
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OVERSENDT, 2, LocalDate.now().minusDays(1), LocalDateTime.now()));
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OVERSENDT, 2, LocalDate.now().plusDays(1), LocalDateTime.now()));
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OVERSENDT, 0, LocalDate.now().minusDays(1), LocalDateTime.now()));
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OVERSENDT, 3, LocalDate.now().minusDays(1), LocalDateTime.now()));
		notifikasjonRepository.saveAll(notifikasjonList);

		List<Notifikasjon> notifikasjonsForSnot001 = notifikasjonRepository.findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoIsLessThanEqual(OVERSENDT, 0, now());

		assertEquals(2, notifikasjonsForSnot001.size());
		notifikasjonsForSnot001.forEach(notifikasjon -> {
			assertEquals(OVERSENDT, notifikasjon.getStatus());
			assertTrue(notifikasjon.getAntallRenotifikasjoner() > 0);
			assertTrue(LocalDate.now().isAfter(notifikasjon.getNesteRenotifikasjonDato()));
		});
	}

	@Test
	public void shouldGetNotifikasjonDistribusjonForSnot001() {
		Notifikasjon notifikasjon_1 = createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OVERSENDT, 3, now().minusDays(1), LocalDateTime.now());
		Notifikasjon notifikasjon_2 = createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OVERSENDT, 3, now().minusDays(1), LocalDateTime.now());
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjon(notifikasjon_1));
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjon(notifikasjon_2));

		List<Notifikasjon> notifikasjonList = new ArrayList<>();
		notifikasjonList.add(notifikasjon_1);
		notifikasjonList.add(notifikasjon_2);

		assertEquals(2, notifikasjonDistribusjonRepository.findAllByNotifikasjonIn(notifikasjonList).size());
	}


	@Test
	public void shouldGetNotifikasjonerForSnot002() {
		List<Notifikasjon> notifikasjonList = new ArrayList<>();
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(FEILET, 2, LocalDate.now(), LocalDateTime.now()));

		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OPPRETTET, 2, LocalDate.now(), null));
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OPPRETTET, 0, LocalDate.now(), null));
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OPPRETTET, null, LocalDate.now(), null));


		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OVERSENDT, 2, LocalDate.now(), LocalDateTime.now()));
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OVERSENDT, 0, LocalDate.now(), LocalDateTime.now()));
		notifikasjonList.add(createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OVERSENDT, null, LocalDate.now(), LocalDateTime.now()));
		notifikasjonRepository.saveAll(notifikasjonList);

		List<Notifikasjon> notifikasjonerForSnot002 = notifikasjonRepository.findAllWithStatusOpprettetOrOversendtAndNoRenotifikasjoner();

		assertEquals(4, notifikasjonerForSnot002.size());
		notifikasjonerForSnot002.forEach(notifikasjon -> {
			assertTrue(List.of(OPPRETTET, OVERSENDT).contains(notifikasjon.getStatus()));
			assertTrue(notifikasjon.getAntallRenotifikasjoner() == null || notifikasjon.getAntallRenotifikasjoner() == 0);
		});
	}

	@Test
	public void shouldGetNotifikasjonDistribusjonForSnot002() {
		Notifikasjon notifikasjon = createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(OVERSENDT, 3, LocalDate.now().minusDays(1), LocalDateTime.now());
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjon(notifikasjon, ENDRET_DATO_2.minusDays(10)));
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjon(notifikasjon, ENDRET_DATO_2.minusDays(2)));

		notifikasjonRepository.save(notifikasjon);

		assertEquals(ENDRET_DATO_2.minusDays(2), notifikasjonDistribusjonRepository.findFirstByNotifikasjonAndKanalAndEndretDatoIsNotNullOrderByEndretDatoDesc(notifikasjon, SMS).get().getEndretDato());
	}

	private NotifikasjonDistribusjon createNotifikasjonDistribusjonWithNotifikasjon(Notifikasjon notifikasjon) {
		return this.createNotifikasjonDistribusjonWithNotifikasjon(notifikasjon, ENDRET_DATO_2);
	}

	private NotifikasjonDistribusjon createNotifikasjonDistribusjonWithNotifikasjon(Notifikasjon notifikasjon, LocalDateTime sistEndret) {
		return NotifikasjonDistribusjon.builder()
				.notifikasjon(notifikasjon)
				.status(FERDIGSTILT)
				.kanal(SMS)
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

	private Notifikasjon createNotifikasjon() {
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

	private Notifikasjon createNotifikasjonWithStatusAntallRenotifikasjonerAndNesteRenotifikasjonDato(Status status, Integer antallRenotifikasjoner, LocalDate nesteRenotifikasjonsDato, LocalDateTime endretDato) {
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
				.opprettetDato(LocalDateTime.now())
				.endretAv(ENDRET_AV)
				.endretDato(endretDato)
				.build();
	}
}
