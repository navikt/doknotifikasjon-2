package no.nav.doknotifikasjon.knot003.itest;


import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.standalonenotificationbe._2009._10.StandaloneNotificationBEList;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaTopics;
import no.nav.doknotifikasjon.knot003.itest.utils.DoknotifikasjonStatusMatcher;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.EmbededKafkaBroker;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.fail;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST;
import static no.nav.doknotifikasjon.knot003.itest.utils.TestUtils.KONTAKTINFO;
import static no.nav.doknotifikasjon.knot003.itest.utils.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.knot003.itest.utils.TestUtils.createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus;
import static no.nav.doknotifikasjon.knot003.itest.utils.TestUtils.generateAltinnResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Knot003ITest extends EmbededKafkaBroker {

	@Autowired
	private NotifikasjonRepository notifikasjonRepository;

	@Autowired
	private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@SpyBean
	private KafkaEventProducer kafkaEventProducer;

	@MockBean
	private INotificationAgencyExternalBasic iNotificationAgencyExternalBasic;

	@BeforeEach
	void setup() {
		notifikasjonDistribusjonRepository.deleteAll();
		notifikasjonRepository.deleteAll();
		reset(kafkaEventProducer);
	}

	@Test
	void shouldSetFerdigstilltStatusOnHappyPath() throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {

		when(iNotificationAgencyExternalBasic.sendStandaloneNotificationBasicV3(anyString(), anyString(), any(StandaloneNotificationBEList.class))).thenReturn(generateAltinnResponse(TransportType.EMAIL, KONTAKTINFO));

		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), Status.OPPRETTET, Kanal.EPOST));

		Integer id = notifikasjonDistribusjon.getId();

		DoknotifikasjonEpost doknotifikasjonEpost = new DoknotifikasjonEpost(id);
		putMessageOnKafkaTopic(doknotifikasjonEpost);

		NotifikasjonDistribusjon updatedNotifikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(id).orElseThrow(() -> new RuntimeException("Failed test!"));


		assertEquals(Status.FERDIGSTILT, updatedNotifikasjonDistribusjon.getStatus());
		assertEquals(Kanal.EPOST, updatedNotifikasjonDistribusjon.getKanal());
		assertEquals("teamdokumenthandtering", updatedNotifikasjonDistribusjon.getEndretAv());
		assertNotNull(updatedNotifikasjonDistribusjon.getEndretDato());

		verify(kafkaEventProducer).publish(
				eq(KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS),
				argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FERDIGSTILT", "notifikasjon sendt via epost", id))
		);
	}

	@Test
	void shouldAbortIfDistribusjonNotFound() throws InterruptedException {

		DoknotifikasjonEpost doknotifikasjonEpost = new DoknotifikasjonEpost(1234);
		putMessageOnKafkaTopic(doknotifikasjonEpost);

		NotifikasjonDistribusjon updatedNotifikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(1234).orElse(null);

		assertNull(updatedNotifikasjonDistribusjon);
		TimeUnit.SECONDS.sleep(2);
		try {
			verify(kafkaEventProducer, times(0)).publish(
					eq(KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS),
					any()
			);
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	void shouldWriteToStatusQueueIfStatusIsInvalid() {

		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), Status.OVERSENDT, Kanal.EPOST));

		Integer id = notifikasjonDistribusjon.getId();

		DoknotifikasjonEpost doknotifikasjonEpost = new DoknotifikasjonEpost(id);
		putMessageOnKafkaTopic(doknotifikasjonEpost);

		verify(kafkaEventProducer, atLeastOnce()).publish(
				eq(KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS),
				argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FEILET", "distribusjon til epost feilet: ugyldig status", id))
		);
	}

	@Test
	void shouldWriteToStatusQueueIfKanalIsInvalid() {

		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), Status.OPPRETTET, Kanal.SMS));

		Integer id = notifikasjonDistribusjon.getId();

		DoknotifikasjonEpost doknotifikasjonEpost = new DoknotifikasjonEpost(id);
		putMessageOnKafkaTopic(doknotifikasjonEpost);

		verify(kafkaEventProducer, atLeastOnce()).publish(
				eq(KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS),
				argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FEILET", "distribusjon til epost feilet: ugyldig kanal", id))
		);
	}

	@Test
	void shouldWriteToStatusQueueIfAltinnThrowsFunctionalError() throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {

		when(iNotificationAgencyExternalBasic.sendStandaloneNotificationBasicV3(anyString(), anyString(), any(StandaloneNotificationBEList.class))).thenThrow(new INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage("Altinn Functional Exception"));

		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), Status.OPPRETTET, Kanal.EPOST));

		Integer id = notifikasjonDistribusjon.getId();

		DoknotifikasjonEpost doknotifikasjonEpost = new DoknotifikasjonEpost(id);
		putMessageOnKafkaTopic(doknotifikasjonEpost);

		notifikasjonDistribusjonRepository.findById(id).orElseThrow(() -> new RuntimeException("Failed test!"));


		verify(kafkaEventProducer, atLeastOnce()).publish(
				eq(KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS),
				argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FEILET", "Feil av typen INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage ved kall mot Altinn. Feilmelding: Altinn Functional Exception", id))
		);
	}

	private void putMessageOnKafkaTopic(DoknotifikasjonEpost doknotifikasjonEpost) {
		try {
			kafkaEventProducer.publish(
					KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST,
					doknotifikasjonEpost
			);

			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException exception) {
			fail();
		}
	}
}