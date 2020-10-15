package no.nav.doknotifikasjon.itest;

import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLER_ID_2;
import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLING_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.MELDING;
import static no.nav.doknotifikasjon.utils.TestUtils.STATUS;
import static no.nav.doknotifikasjon.utils.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import no.nav.doknotifikasjon.KafkaProducer.KafkaEventProducer;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

public class Knot004ITest extends EmbededKafkaBroker {

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
	public void shouldUpdateStatus() throws InterruptedException {
		notifikasjonRepository.saveAndFlush(createNotifikasjon());

		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLING_ID, BESTILLER_ID_2, STATUS, MELDING, null);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingId(BESTILLING_ID);
		assertEquals(STATUS, updatedNotifikasjon.getStatus().toString());
		assertEquals(BESTILLER_ID_2, updatedNotifikasjon.getEndretAv());
		assertNotNull(updatedNotifikasjon.getEndretDato());
	}

	@Test
	public void sholdNotUpdateWhenNotifikasjonDoesNotExist() throws InterruptedException {
		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLING_ID, BESTILLER_ID_2, STATUS, MELDING, null);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		assertNull(notifikasjonRepository.findByBestillingId(BESTILLING_ID));
	}

	private void putMessageOnKafkaTopic(DoknotifikasjonStatus doknotifikasjonStatus) throws InterruptedException {
		Long keyGenerator = System.currentTimeMillis();

		KafkaEventProducer.publish(
				KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
				doknotifikasjonStatus,
				keyGenerator
		);

		TimeUnit.SECONDS.sleep(30);
	}
}
