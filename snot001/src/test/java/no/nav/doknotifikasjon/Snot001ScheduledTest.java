package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.EmbededKafkaBroker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static no.nav.doknotifikasjon.TestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class Snot001ScheduledTest extends EmbededKafkaBroker {

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
    void shouldNotResendNotifikasjonWhenNoNotifikasjonToResend() {

        notifikasjonRepository.saveAndFlush(TestUtils.createNotifikasjonWithStatus(Status.FERDIGSTILT));
        snot001Scheduler.scheduledJob();

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);

        assertEquals(ANTALL_RENOTIFIKASJONER, updatedNotifikasjon.getAntallRenotifikasjoner());
        assertEquals(NESTE_RENOTIFIKASJONS_DATO, updatedNotifikasjon.getNesteRenotifikasjonDato());
        assertNull(updatedNotifikasjon.getEndretAv());
        assertNull(updatedNotifikasjon.getEndretDato());
        assertTrue(updatedNotifikasjon.getNotifikasjonDistribusjon().isEmpty());
    }

    @Test
    void shouldResendNotifikasjonDistribusjon() {
        Notifikasjon notifikasjon = createNotifikasjon();
        notifikasjon.setNotifikasjonDistribusjon(Collections.singleton(TestUtils.createNotifikasjonDistribusjonWithNotifikasjonAndKanal(notifikasjon, Kanal.EPOST)));
        notifikasjon.setNotifikasjonDistribusjon(Collections.singleton(TestUtils.createNotifikasjonDistribusjonWithNotifikasjonAndKanal(notifikasjon, Kanal.SMS)));

        notifikasjonRepository.saveAndFlush(notifikasjon);
        snot001Scheduler.scheduledJob();

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
        LocalDate now = LocalDate.now();

        assertEquals(ANTALL_RENOTIFIKASJONER - 1, updatedNotifikasjon.getAntallRenotifikasjoner());
        assertEquals(now.plusDays(updatedNotifikasjon.getRenotifikasjonIntervall()), updatedNotifikasjon.getNesteRenotifikasjonDato());
        assertEquals(SNOT001, updatedNotifikasjon.getEndretAv());
        assertNotNull(updatedNotifikasjon.getEndretDato());

        List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAllByNotifikasjonAndStatus(updatedNotifikasjon, Status.OPPRETTET);

        notifikasjonDistribusjonList.forEach(notifikasjonDistribusjon -> {
            assertEquals(Status.OPPRETTET, notifikasjonDistribusjon.getStatus());
            assertEquals(KONTAKTINFO, notifikasjonDistribusjon.getKontaktInfo());
            assertEquals(TITTEL, notifikasjonDistribusjon.getTittel());
            assertEquals(PAAMINNELSE_TEKST, notifikasjonDistribusjon.getTekst());
            assertEquals(SNOT001, notifikasjonDistribusjon.getOpprettetAv());
            assertNotNull(notifikasjonDistribusjon.getOpprettetDato());
            assertThat(notifikasjonDistribusjon.getKanal(), anyOf(is(Kanal.SMS), is(Kanal.EPOST)));
        });
    }
}
