package no.nav.doknotifikasjon.repository;

import static org.junit.Assert.assertEquals;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.MottakerIdType;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.junit.Test;
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
	private static final String ENDRET_AV = "srvdokumentlosninger";
	private static final Integer ANTALL_RENOTIFIKASJONER = 2;
	private static final Integer RENOTIFIKASJON_INTERVALL = 7;
	private static final LocalDate NESTE_RENOTIFIKASJON_DATO = LocalDate.parse("2020-10-03");
	private static final LocalDateTime OPPRETTET_DATO = LocalDateTime.parse("2020-10-01T10:15:30.000000");
	private static final LocalDateTime ENDRET_DATO = LocalDateTime.parse("2020-10-02T10:15:30.000000");


	@Autowired
	NotifikasjonRepository notifikasjonRepository;

	@Autowired
	NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@Test
	public void shouldOpprettNotifikasjon() {
		assertEquals(0L, notifikasjonRepository.count());
		notifikasjonRepository.saveAndFlush(createNotifikasjon());

		assertEquals(1L, notifikasjonRepository.count());

		Notifikasjon notfikasjon = notifikasjonRepository.findByNotifikasjonId(1);
		assertEquals(BESTILLING_ID, notfikasjon.getBestillingId());
		assertEquals(BESTILLER_ID, notfikasjon.getBestillerId());
		assertEquals(MOTTAKER_ID, notfikasjon.getMottakerId());
		assertEquals(PREFERERTE_KANALER, notfikasjon.getPrefererteKanaler());
		assertEquals(OPPRETTET_AV, notfikasjon.getOpprettetAv());
		assertEquals(ANTALL_RENOTIFIKASJONER, notfikasjon.getAntallRenotifikasjoner());
		assertEquals(RENOTIFIKASJON_INTERVALL, notfikasjon.getRenotifikasjonIntervall());
		assertEquals(NESTE_RENOTIFIKASJON_DATO, notfikasjon.getNesteRenotifikasjonDato());
		assertEquals(OPPRETTET_DATO, notfikasjon.getOpprettetDato());
		assertEquals(ENDRET_DATO, notfikasjon.getEndretDato());
	}

	@Test
	public void shouldOpprettNotifikasjonDistribusjon(){
		assertEquals(0L, notifikasjonDistribusjonRepository.count());
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonId(1));

		assertEquals(1L, notifikasjonRepository.count());
	}

	private NotifikasjonDistribusjon createNotifikasjonDistribusjonWithNotifikasjonId(Integer notifikasjonId) {
		return NotifikasjonDistribusjon.builder()
				.notifikasjonId(notifikasjonId)
				
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
