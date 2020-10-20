package no.nav.doknotifikasjon.itest;

import no.nav.doknotifikasjon.KafkaProducer.KafkaEventProducer;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS;
import static no.nav.doknotifikasjon.utils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Knot004ITest extends EmbededKafkaBroker {

    @Autowired
    private KafkaEventProducer KafkaEventProducer;

    @Autowired
    private NotifikasjonRepository notifikasjonRepository;

    @Autowired
    private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

    @BeforeEach
    public void setup() {
        notifikasjonDistribusjonRepository.deleteAll();
        notifikasjonRepository.deleteAll();
    }

    @Test
    void shouldUpdateStatus() {
        notifikasjonRepository.saveAndFlush(createNotifikasjon());

        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, STATUS_OPPRETTET_STRING, MELDING, null);
        putMessageOnKafkaTopic(doknotifikasjonStatus);

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
        assertEquals(STATUS_OPPRETTET, updatedNotifikasjon.getStatus());
        assertEquals(BESTILLER_ID_2, updatedNotifikasjon.getEndretAv());
        assertNotNull(updatedNotifikasjon.getEndretDato());
    }

    @Test
    void shouldNotUpdateStatusWhenNotifikasjonDoesNotExist() {
        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, STATUS_OPPRETTET_STRING, MELDING, null);
        putMessageOnKafkaTopic(doknotifikasjonStatus);

        assertNull(notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID));
    }

    @Test
    void shouldNotUpdateStatusWhenStatusIsFerdigstiltAndAntallRenotifikasjonerIsMoreThanZero() {
        notifikasjonRepository.saveAndFlush(createNotifikasjon());

        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, STATUS_FERDIGSTILT_STRING, MELDING, null);
        putMessageOnKafkaTopic(doknotifikasjonStatus);

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
        assertEquals(Status.FEILET.toString(), updatedNotifikasjon.getStatus().toString());
        assertNull(updatedNotifikasjon.getEndretAv());
        assertNull(updatedNotifikasjon.getEndretDato());
    }

    @Test
    void shouldNotUpdateStatusWhenDistribusjonIdIsNotZero() {
        notifikasjonRepository.saveAndFlush(createNotifikasjon());

        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, STATUS_OPPRETTET_STRING, MELDING, DISTRIBUSJON_ID);
        putMessageOnKafkaTopic(doknotifikasjonStatus);

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
        assertEquals(Status.FEILET.toString(), updatedNotifikasjon.getStatus().toString());
        assertNull(updatedNotifikasjon.getEndretAv());
        assertNull(updatedNotifikasjon.getEndretDato());
    }

    @Test
    void shouldPublishNewHendelseWhenDistribusjonIdIsNotZeroAndInputStatusEqualsNotifikasjonStatus() {
        notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), Status.OPPRETTET));

        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, STATUS_OPPRETTET_STRING, MELDING, DISTRIBUSJON_ID);
        putMessageOnKafkaTopic(doknotifikasjonStatus);

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
        assertEquals(Status.OPPRETTET, updatedNotifikasjon.getStatus());
        assertEquals(BESTILLER_ID_2, updatedNotifikasjon.getEndretAv());
        assertNotNull(updatedNotifikasjon.getEndretDato());
    }

    @Test
    void shouldNotPublishNewHendelseWhenDistribusjonIdIsNotZeroAndInputStatusNotEqualsOneNotifikasjonStatus() {
        Notifikasjon notifikasjon = createNotifikasjon();
        NotifikasjonDistribusjon notifikasjonDistribusjon_1 = createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(notifikasjon, Status.OPPRETTET);
        NotifikasjonDistribusjon notifikasjonDistribusjon_2 = createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(notifikasjon, Status.FEILET);
        notifikasjon.setNotifikasjonDistribusjon(Set.of(notifikasjonDistribusjon_1, notifikasjonDistribusjon_2));

        notifikasjonRepository.saveAndFlush(notifikasjon);

        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, STATUS_OPPRETTET_STRING, MELDING, DISTRIBUSJON_ID);
        putMessageOnKafkaTopic(doknotifikasjonStatus);

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
        assertEquals(Status.FEILET.toString(), updatedNotifikasjon.getStatus().toString());
        assertNull(updatedNotifikasjon.getEndretAv());
        assertNull(updatedNotifikasjon.getEndretDato());
    }

    private void putMessageOnKafkaTopic(DoknotifikasjonStatus doknotifikasjonStatus) {
        try {
            Long keyGenerator = System.currentTimeMillis();

            KafkaEventProducer.publish(
                    KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
                    doknotifikasjonStatus,
                    keyGenerator
            );

            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException exception) {
            fail();
        }
    }
}
