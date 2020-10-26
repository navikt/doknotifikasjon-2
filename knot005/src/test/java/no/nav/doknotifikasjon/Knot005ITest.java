package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.EmbededKafkaBroker;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static no.nav.doknotifikasjon.TestUtils.*;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STOPP;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Knot005ITest extends EmbededKafkaBroker {

    @Autowired
    private KafkaEventProducer KafkaEventProducer;

    @Autowired
    private NotifikasjonRepository notifikasjonRepository;

    @BeforeEach
    public void setup() {
        notifikasjonRepository.deleteAll();
    }

    @Test
    void shouldUpdateStatus() {
        notifikasjonRepository.saveAndFlush(createNotifikasjonWithStatus(Status.OPPRETTET));

        DoknotifikasjonStopp doknotifikasjonStopp = new DoknotifikasjonStopp(BESTILLINGS_ID, BESTILLER_ID_2);
        putMessageOnKafkaTopic(doknotifikasjonStopp);

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
        assertEquals(0, updatedNotifikasjon.getAntallRenotifikasjoner());
        assertNull(updatedNotifikasjon.getNesteRenotifikasjonDato());
        assertEquals(BESTILLER_ID_2, updatedNotifikasjon.getEndretAv());
        assertNotNull(updatedNotifikasjon.getEndretDato());
    }

    @Test
    void shouldNotUpdateStatusWhenNotifikasjonDoesNotExist() {
        DoknotifikasjonStopp doknotifikasjonStopp = new DoknotifikasjonStopp(BESTILLINGS_ID, BESTILLER_ID_2);
        putMessageOnKafkaTopic(doknotifikasjonStopp);

        assertNull(notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID));
    }

    @Test
    void shouldNotUpdateStatusWhenNotifikasjonHasStatusFerdigstilt() {
        notifikasjonRepository.saveAndFlush(createNotifikasjonWithStatus(Status.FERDIGSTILT));

        DoknotifikasjonStopp doknotifikasjonStopp = new DoknotifikasjonStopp(BESTILLINGS_ID, BESTILLER_ID_2);
        putMessageOnKafkaTopic(doknotifikasjonStopp);

        Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
        assertEquals(ANTALL_RENOTIFIKASJONER, updatedNotifikasjon.getAntallRenotifikasjoner());
        assertEquals(NESTE_RENOTIFIKASJONSDATO, updatedNotifikasjon.getNesteRenotifikasjonDato());
        assertEquals(BESTILLER_ID, updatedNotifikasjon.getBestillerId());
        assertNull(updatedNotifikasjon.getEndretAv());
        assertNull(updatedNotifikasjon.getEndretDato());
    }

    private void putMessageOnKafkaTopic(DoknotifikasjonStopp doknotifikasjonStopp) {
        try {
            Long keyGenerator = System.currentTimeMillis();

            KafkaEventProducer.publish(
                    KAFKA_TOPIC_DOK_NOTIFIKASJON_STOPP,
                    doknotifikasjonStopp,
                    keyGenerator
            );

            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException exception) {
            fail();
        }
    }
}
