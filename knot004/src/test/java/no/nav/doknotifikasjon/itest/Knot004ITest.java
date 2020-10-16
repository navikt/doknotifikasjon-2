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
import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLER_ID_2;
import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLING_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.DISTRIBUSJON_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.MELDING;
import static no.nav.doknotifikasjon.utils.TestUtils.STATUS_FERDIGSTILT;
import static no.nav.doknotifikasjon.utils.TestUtils.STATUS_OPPRETTET;
import static no.nav.doknotifikasjon.utils.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.utils.TestUtils.createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

class Knot004ITest extends EmbededKafkaBroker {

    @Autowired
    private KafkaEventProducer KafkaEventProducer;

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
    void shouldUpdateStatus() {
        notifikasjonRepository.saveAndFlush(createNotifikasjon());

        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLING_ID, BESTILLER_ID_2, STATUS_OPPRETTET, MELDING, null);
        putMessageOnKafkaTopic(doknotifikasjonStatus);

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingId(BESTILLING_ID);
        assertEquals(STATUS_OPPRETTET, updatedNotifikasjon.getStatus().toString());
        assertEquals(BESTILLER_ID_2, updatedNotifikasjon.getEndretAv());
        assertNotNull(updatedNotifikasjon.getEndretDato());
    }

    @Test
    void shouldNotUpdateStatusWhenNotifikasjonDoesNotExist() {
        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLING_ID, BESTILLER_ID_2, STATUS_OPPRETTET, MELDING, null);
        putMessageOnKafkaTopic(doknotifikasjonStatus);

        assertNull(notifikasjonRepository.findByBestillingId(BESTILLING_ID));
    }

    @Test
    void shouldNotUpdateStatusWhenStatusIsFerdigstiltAndAntallRenotifikasjonerIsMoreThanZero() {
        notifikasjonRepository.saveAndFlush(createNotifikasjon());

        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLING_ID, BESTILLER_ID_2, STATUS_FERDIGSTILT, MELDING, null);
        putMessageOnKafkaTopic(doknotifikasjonStatus);

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingId(BESTILLING_ID);
        assertEquals(Status.FEILET.toString(), updatedNotifikasjon.getStatus().toString());
        assertNull(updatedNotifikasjon.getEndretAv());
        assertNull(updatedNotifikasjon.getEndretDato());
    }

    @Test
    void shouldNotUpdateStatusWhenDistribusjonIdIsNotZero() {
        notifikasjonRepository.saveAndFlush(createNotifikasjon());

        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLING_ID, BESTILLER_ID_2, STATUS_OPPRETTET, MELDING, DISTRIBUSJON_ID);
        putMessageOnKafkaTopic(doknotifikasjonStatus);

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingId(BESTILLING_ID);
        assertEquals(Status.FEILET.toString(), updatedNotifikasjon.getStatus().toString());
        assertNull(updatedNotifikasjon.getEndretAv());
        assertNull(updatedNotifikasjon.getEndretDato());
    }

    @Test
    void shouldPublishNewHendelseWhenDistribusjonIdIsNotZeroAndInputStatusEqualsNotifikasjonStatus() {
        notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), Status.OPPRETTET));

        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLING_ID, BESTILLER_ID_2, STATUS_OPPRETTET, MELDING, DISTRIBUSJON_ID);
        putMessageOnKafkaTopic(doknotifikasjonStatus);

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingId(BESTILLING_ID);
        assertEquals(Status.OPPRETTET, updatedNotifikasjon.getStatus());
        assertEquals(BESTILLER_ID_2, updatedNotifikasjon.getEndretAv());
        assertNotNull(updatedNotifikasjon.getEndretDato());
    }

    @Test
    public void shouldNotPublishNewHendelseWhenDistribusjonIdIsNotZeroAndInputStatusNotEqualsOneNotifikasjonStatus() {
        Notifikasjon notifikasjon = createNotifikasjon();
        NotifikasjonDistribusjon notifikasjonDistribusjon_1 = createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(notifikasjon, Status.OPPRETTET);
        NotifikasjonDistribusjon notifikasjonDistribusjon_2 = createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(notifikasjon, Status.FEILET);
        notifikasjon.setNotifikasjonDistribusjon(Set.of(notifikasjonDistribusjon_1, notifikasjonDistribusjon_2));

        notifikasjonRepository.saveAndFlush(notifikasjon);

        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLING_ID, BESTILLER_ID_2, STATUS_OPPRETTET, MELDING, DISTRIBUSJON_ID);
        putMessageOnKafkaTopic(doknotifikasjonStatus);

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingId(BESTILLING_ID);
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
