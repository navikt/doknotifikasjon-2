package no.nav.doknotifikasjon.itest;

import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.EmbededKafkaBroker;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS;
import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static no.nav.doknotifikasjon.kodeverk.Status.INFO;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;
import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLER_ID_2;
import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLINGS_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.DISTRIBUSJON_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.MELDING;
import static no.nav.doknotifikasjon.utils.TestUtils.STATUS_OPPRETTET;
import static no.nav.doknotifikasjon.utils.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.utils.TestUtils.createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, OPPRETTET.toString(), MELDING, null);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
			assertEquals(STATUS_OPPRETTET, updatedNotifikasjon.getStatus());
			assertEquals(BESTILLER_ID_2, updatedNotifikasjon.getEndretAv());
			assertNotNull(updatedNotifikasjon.getEndretDato());
		});
	}

	@Test
	void shouldNotUpdateStatusWhenInputStatusIsInfo() {
		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, INFO.toString(), MELDING, null);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		await().atMost(10, SECONDS).untilAsserted(() ->
				assertNull(notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID))
		);
	}

	@Test
	void shouldNotUpdateStatusWhenNotifikasjonDoesNotExist() {
		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, OPPRETTET.toString(), MELDING, null);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		await().atMost(10, SECONDS).untilAsserted(() ->
				assertNull(notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID))
		);
	}

	@Test
	void shouldNotUpdateStatusWhenStatusIsFerdigstiltAndAntallRenotifikasjonerIsMoreThanZero() {
		notifikasjonRepository.saveAndFlush(createNotifikasjon());

		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, FERDIGSTILT.toString(), MELDING, null);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
			assertEquals(FEILET.toString(), updatedNotifikasjon.getStatus().toString());
			assertNull(updatedNotifikasjon.getEndretAv());
			assertNull(updatedNotifikasjon.getEndretDato());
		});
	}

	@Test
	void shouldNotUpdateStatusWhenDistribusjonIdIsNotZero() {
		notifikasjonRepository.saveAndFlush(createNotifikasjon());

		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, OPPRETTET.toString(), MELDING, DISTRIBUSJON_ID);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
			assertEquals(FEILET.toString(), updatedNotifikasjon.getStatus().toString());
			assertNull(updatedNotifikasjon.getEndretAv());
			assertNull(updatedNotifikasjon.getEndretDato());
		});
	}

	@Test
	void shouldPublishNewHendelseWhenDistribusjonIdIsNotZeroAndInputStatusEqualsNotifikasjonStatus() {
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), OPPRETTET));

		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, OPPRETTET.toString(), MELDING, DISTRIBUSJON_ID);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
			assertEquals(OPPRETTET, updatedNotifikasjon.getStatus());
			assertEquals(BESTILLER_ID_2, updatedNotifikasjon.getEndretAv());
			assertNotNull(updatedNotifikasjon.getEndretDato());
		});
	}

	@Test
	void shouldNotPublishNewHendelseWhenDistribusjonIdIsNotZeroAndInputStatusNotEqualsOneNotifikasjonStatus() {
		Notifikasjon notifikasjon = createNotifikasjon();
		NotifikasjonDistribusjon notifikasjonDistribusjon_1 = createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(notifikasjon, OPPRETTET);
		NotifikasjonDistribusjon notifikasjonDistribusjon_2 = createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(notifikasjon, FEILET);
		notifikasjon.setNotifikasjonDistribusjon(Set.of(notifikasjonDistribusjon_1, notifikasjonDistribusjon_2));

		notifikasjonRepository.saveAndFlush(notifikasjon);

		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, OPPRETTET.toString(), MELDING, DISTRIBUSJON_ID);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
			assertEquals(FEILET.toString(), updatedNotifikasjon.getStatus().toString());
			assertNull(updatedNotifikasjon.getEndretAv());
			assertNull(updatedNotifikasjon.getEndretDato());
		});
	}

	private void putMessageOnKafkaTopic(DoknotifikasjonStatus doknotifikasjonStatus) {
		KafkaEventProducer.publish(
				KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
				doknotifikasjonStatus
		);
	}
}
