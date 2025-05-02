package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.AbstractKafkaBrokerTest;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.doknotifikasjon.TestUtils.ANTALL_RENOTIFIKASJONER;
import static no.nav.doknotifikasjon.TestUtils.BESTILLER_ID;
import static no.nav.doknotifikasjon.TestUtils.BESTILLER_ID_2;
import static no.nav.doknotifikasjon.TestUtils.BESTILLINGS_ID;
import static no.nav.doknotifikasjon.TestUtils.NESTE_RENOTIFIKASJONSDATO;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjonWithStatus;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STOPP;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Knot005ITest extends AbstractKafkaBrokerTest {

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

		await().atMost(10, SECONDS).untilAsserted(() -> {
			Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID).orElse(null);
			assertNotNull(updatedNotifikasjon);
			assertEquals(0, updatedNotifikasjon.getAntallRenotifikasjoner());
			assertNull(updatedNotifikasjon.getNesteRenotifikasjonDato());
			assertEquals(BESTILLER_ID_2, updatedNotifikasjon.getEndretAv());
			assertEquals(FERDIGSTILT, updatedNotifikasjon.getStatus());
			assertNotNull(updatedNotifikasjon.getEndretDato());
		});
	}

	@Test
	void shouldNotUpdateStatusWhenNotifikasjonDoesNotExist() {
		DoknotifikasjonStopp doknotifikasjonStopp = new DoknotifikasjonStopp(BESTILLINGS_ID, BESTILLER_ID_2);
		putMessageOnKafkaTopic(doknotifikasjonStopp);

		await().pollDelay(2, SECONDS).atMost(10, SECONDS).untilAsserted(() ->
			assertTrue(notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID).isEmpty())
		);
	}

	@Test
	void shouldNotUpdateStatusWhenNotifikasjonHasStatusFerdigstilt() {
		notifikasjonRepository.saveAndFlush(createNotifikasjonWithStatus(FERDIGSTILT));

		DoknotifikasjonStopp doknotifikasjonStopp = new DoknotifikasjonStopp(BESTILLINGS_ID, BESTILLER_ID_2);
		putMessageOnKafkaTopic(doknotifikasjonStopp);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID).orElse(null);
			assertNotNull(updatedNotifikasjon);
			assertEquals(ANTALL_RENOTIFIKASJONER, updatedNotifikasjon.getAntallRenotifikasjoner());
			assertEquals(NESTE_RENOTIFIKASJONSDATO, updatedNotifikasjon.getNesteRenotifikasjonDato());
			assertEquals(BESTILLER_ID, updatedNotifikasjon.getBestillerId());
			assertEquals(FERDIGSTILT, updatedNotifikasjon.getStatus());
			assertNull(updatedNotifikasjon.getEndretAv());
			assertNull(updatedNotifikasjon.getEndretDato());
		});
	}

	private void putMessageOnKafkaTopic(DoknotifikasjonStopp doknotifikasjonStopp) {
		KafkaEventProducer.publish(
				KAFKA_TOPIC_DOK_NOTIFIKASJON_STOPP,
				doknotifikasjonStopp
		);
	}
}
