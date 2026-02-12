package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.AbstractKafkaBrokerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static no.nav.doknotifikasjon.TestUtils.ANTALL_RENOTIFIKASJONER;
import static no.nav.doknotifikasjon.TestUtils.BESTILLINGS_ID;
import static no.nav.doknotifikasjon.TestUtils.KONTAKTINFO;
import static no.nav.doknotifikasjon.TestUtils.NESTE_RENOTIFIKASJONS_DATO;
import static no.nav.doknotifikasjon.TestUtils.PAAMINNELSE_TEKST;
import static no.nav.doknotifikasjon.TestUtils.SNOT001;
import static no.nav.doknotifikasjon.TestUtils.TITTEL;
import static no.nav.doknotifikasjon.TestUtils.createDefaultNotifikasjon;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjonDistribusjonWithNotifikasjonAndKanal;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjonWithAntallRenotifikasjoner;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjonWithStatus;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("altinn2")
class Snot001ScheduledTest extends AbstractKafkaBrokerTest {

	@Autowired
	private NotifikasjonRepository notifikasjonRepository;

	@Autowired
	private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@Autowired
	private Snot001Scheduler snot001Scheduler;

	@BeforeEach
	public void setup() {
		notifikasjonDistribusjonRepository.deleteAll();
		notifikasjonRepository.deleteAll();
	}

	@Test
	void shouldResendNotifikasjonDistribusjonWithAllKanal() {
		Notifikasjon notifikasjon = createDefaultNotifikasjon();
		Set<NotifikasjonDistribusjon> notifikasjonDistribusjon = new HashSet<>();
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonAndKanal(notifikasjon, SMS));
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonAndKanal(notifikasjon, EPOST));

		notifikasjon.setNotifikasjonDistribusjon(notifikasjonDistribusjon);

		notifikasjonRepository.saveAndFlush(notifikasjon);
		snot001Scheduler.scheduledJob();

		Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID).orElse(null);
		LocalDate now = LocalDate.now();

		assertEquals(ANTALL_RENOTIFIKASJONER - 1, updatedNotifikasjon.getAntallRenotifikasjoner());
		assertEquals(now.plusDays(updatedNotifikasjon.getRenotifikasjonIntervall()), updatedNotifikasjon.getNesteRenotifikasjonDato());
		assertEquals(SNOT001, updatedNotifikasjon.getEndretAv());
		assertNotNull(updatedNotifikasjon.getEndretDato());

		List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAllByNotifikasjonAndStatus(updatedNotifikasjon, OPPRETTET);

		notifikasjonDistribusjonList.forEach(nd -> {
			assertEquals(OPPRETTET, nd.getStatus());
			assertEquals(KONTAKTINFO, nd.getKontaktInfo());
			assertEquals(TITTEL, nd.getTittel());
			assertEquals(PAAMINNELSE_TEKST, nd.getTekst());
			assertEquals(SNOT001, nd.getOpprettetAv());
			assertNotNull(nd.getOpprettetDato());
			assertThat(nd.getKanal(), anyOf(is(SMS), is(EPOST)));
		});

		assertTrue(notifikasjonDistribusjonList.stream().anyMatch(n -> SMS.equals(n.getKanal())));
		assertTrue(notifikasjonDistribusjonList.stream().anyMatch(n -> EPOST.equals(n.getKanal())));
	}


	@Test
	void shouldResendNotifikasjonDistribusjonWithKanalSMS() {
		Notifikasjon notifikasjon = createDefaultNotifikasjon();
		notifikasjon.setNotifikasjonDistribusjon(Collections.singleton(createNotifikasjonDistribusjonWithNotifikasjonAndKanal(notifikasjon, SMS)));

		notifikasjonRepository.saveAndFlush(notifikasjon);
		snot001Scheduler.scheduledJob();

		Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID).orElse(null);
		LocalDate now = LocalDate.now();

		assertEquals(ANTALL_RENOTIFIKASJONER - 1, updatedNotifikasjon.getAntallRenotifikasjoner());
		assertEquals(now.plusDays(updatedNotifikasjon.getRenotifikasjonIntervall()), updatedNotifikasjon.getNesteRenotifikasjonDato());
		assertEquals(SNOT001, updatedNotifikasjon.getEndretAv());
		assertNotNull(updatedNotifikasjon.getEndretDato());

		List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAllByNotifikasjonAndStatus(updatedNotifikasjon, OPPRETTET);

		notifikasjonDistribusjonList.forEach(notifikasjonDistribusjon -> {
			assertEquals(OPPRETTET, notifikasjonDistribusjon.getStatus());
			assertEquals(KONTAKTINFO, notifikasjonDistribusjon.getKontaktInfo());
			assertEquals(TITTEL, notifikasjonDistribusjon.getTittel());
			assertEquals(PAAMINNELSE_TEKST, notifikasjonDistribusjon.getTekst());
			assertEquals(SNOT001, notifikasjonDistribusjon.getOpprettetAv());
			assertNotNull(notifikasjonDistribusjon.getOpprettetDato());
			assertEquals(SMS, notifikasjonDistribusjon.getKanal());
		});


		assertTrue(notifikasjonDistribusjonList.stream().anyMatch(n -> SMS.equals(n.getKanal())));
		assertFalse(notifikasjonDistribusjonList.stream().anyMatch(n -> EPOST.equals(n.getKanal())));
	}

	@Test
	void shouldResendNotifikasjonDistribusjonWithKanalEpost() {
		Notifikasjon notifikasjon = createDefaultNotifikasjon();
		notifikasjon.setNotifikasjonDistribusjon(Collections.singleton(createNotifikasjonDistribusjonWithNotifikasjonAndKanal(notifikasjon, EPOST)));

		notifikasjonRepository.saveAndFlush(notifikasjon);
		snot001Scheduler.scheduledJob();

		Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID).orElse(null);
		LocalDate now = LocalDate.now();

		assertEquals(ANTALL_RENOTIFIKASJONER - 1, updatedNotifikasjon.getAntallRenotifikasjoner());
		assertEquals(now.plusDays(updatedNotifikasjon.getRenotifikasjonIntervall()), updatedNotifikasjon.getNesteRenotifikasjonDato());
		assertEquals(SNOT001, updatedNotifikasjon.getEndretAv());
		assertNotNull(updatedNotifikasjon.getEndretDato());

		List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAllByNotifikasjonAndStatus(updatedNotifikasjon, OPPRETTET);

		notifikasjonDistribusjonList.forEach(notifikasjonDistribusjon -> {
			assertEquals(OPPRETTET, notifikasjonDistribusjon.getStatus());
			assertEquals(KONTAKTINFO, notifikasjonDistribusjon.getKontaktInfo());
			assertEquals(TITTEL, notifikasjonDistribusjon.getTittel());
			assertEquals(PAAMINNELSE_TEKST, notifikasjonDistribusjon.getTekst());
			assertEquals(SNOT001, notifikasjonDistribusjon.getOpprettetAv());
			assertNotNull(notifikasjonDistribusjon.getOpprettetDato());
			assertEquals(EPOST, notifikasjonDistribusjon.getKanal());
		});


		assertFalse(notifikasjonDistribusjonList.stream().anyMatch(n -> SMS.equals(n.getKanal())));
		assertTrue(notifikasjonDistribusjonList.stream().anyMatch(n -> EPOST.equals(n.getKanal())));
	}

	@Test
	void shouldNotResendNotifikasjonWhenNoNotifikasjonToResend() {

		notifikasjonRepository.saveAndFlush(createNotifikasjonWithStatus(FERDIGSTILT));
		snot001Scheduler.scheduledJob();

		Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID).orElse(null);

		assertEquals(ANTALL_RENOTIFIKASJONER, updatedNotifikasjon.getAntallRenotifikasjoner());
		assertEquals(NESTE_RENOTIFIKASJONS_DATO, updatedNotifikasjon.getNesteRenotifikasjonDato());
		assertNull(updatedNotifikasjon.getEndretAv());
		assertNull(updatedNotifikasjon.getEndretDato());
		assertEquals(0, notifikasjonDistribusjonRepository.findAll().size());
	}

	@Test
	void shouldNotSetNesteRenotifikasjonsDatoWhenAntallRenotifikasjonerIsZero() {
		Notifikasjon notifikasjon = createNotifikasjonWithAntallRenotifikasjoner(1);
		notifikasjon.setNotifikasjonDistribusjon(Collections.singleton(createNotifikasjonDistribusjonWithNotifikasjonAndKanal(notifikasjon, EPOST)));

		notifikasjonRepository.saveAndFlush(notifikasjon);
		snot001Scheduler.scheduledJob();

		Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID).orElse(null);

		assertEquals(0, updatedNotifikasjon.getAntallRenotifikasjoner());
		assertNull(updatedNotifikasjon.getNesteRenotifikasjonDato());
		assertEquals(SNOT001, updatedNotifikasjon.getEndretAv());
		assertNotNull(updatedNotifikasjon.getEndretDato());
	}
}
