package no.nav.doknotifikasjon.knot002.itest;

import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.doknotifikasjon.consumer.altinn.AltinnConsumer;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.knot002.consumer.NotifikasjonDistribusjonConsumer;
import no.nav.doknotifikasjon.knot002.mapper.NotifikasjonEntityMapper;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.EmbededKafkaBroker;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.BESTILLINGS_ID;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.createNotifikasjonWithDistribusjon;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.NOTIFIKASJONDISTRIBUSJONID;

class Knot002ITest extends EmbededKafkaBroker {

	private static WebServiceTemplate mockWebServiceTemplate = Mockito.mock(WebServiceTemplate.class);

	@Autowired
	private static KafkaEventProducer KafkaEventProducer;

	@Autowired
	private static NotifikasjonRepository notifikasjonRepository;

	@Autowired
	private static NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@BeforeEach
	public void setup() {
		notifikasjonDistribusjonRepository.deleteAll();
		notifikasjonRepository.deleteAll();
	}

	@Test
	void shouldUpdateStatus() {
		notifikasjonRepository.saveAndFlush(createNotifikasjonWithDistribusjon(Status.OPPRETTET, NOTIFIKASJONDISTRIBUSJONID));

		new DoknotifikasjonSms(NOTIFIKASJONDISTRIBUSJONID);
		DoknotifikasjonSms doknotifikasjonSms= new DoknotifikasjonSms(NOTIFIKASJONDISTRIBUSJONID);
		putMessageOnKafkaTopic(doknotifikasjonSms);

		NotifikasjonDistribusjon updatedNotifikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(Integer.valueOf(NOTIFIKASJONDISTRIBUSJONID)).orElseThrow(()->new RuntimeException("Failed test!"));


		//Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
		//assertEquals(STATUS_OPPRETTET, updatedNotifikasjon.getStatus());
		//assertEquals(BESTILLER_ID_2, updatedNotifikasjon.getEndretAv());
		//assertNotNull(updatedNotifikasjon.getEndretDato());
	}

/*	@Test
	void shouldNotUpdateStatusWhenInputStatusIsInfo() {
		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, Status.INFO.toString(), MELDING, null);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		assertNull(notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID));
	}

	@Test
	void shouldNotUpdateStatusWhenNotifikasjonDoesNotExist() {
		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, Status.OPPRETTET.toString(), MELDING, null);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		assertNull(notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID));
	}

	@Test
	void shouldNotUpdateStatusWhenStatusIsFerdigstiltAndAntallRenotifikasjonerIsMoreThanZero() {
		notifikasjonRepository.saveAndFlush(createNotifikasjon());

		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, Status.FERDIGSTILT.toString(), MELDING, null);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
		assertEquals(Status.FEILET.toString(), updatedNotifikasjon.getStatus().toString());
		assertNull(updatedNotifikasjon.getEndretAv());
		assertNull(updatedNotifikasjon.getEndretDato());
	}

	@Test
	void shouldNotUpdateStatusWhenDistribusjonIdIsNotZero() {
		notifikasjonRepository.saveAndFlush(createNotifikasjon());

		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, Status.OPPRETTET.toString(), MELDING, DISTRIBUSJON_ID);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
		assertEquals(Status.FEILET.toString(), updatedNotifikasjon.getStatus().toString());
		assertNull(updatedNotifikasjon.getEndretAv());
		assertNull(updatedNotifikasjon.getEndretDato());
	}

	@Test
	void shouldPublishNewHendelseWhenDistribusjonIdIsNotZeroAndInputStatusEqualsNotifikasjonStatus() {
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), Status.OPPRETTET));

		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, Status.OPPRETTET.toString(), MELDING, DISTRIBUSJON_ID);
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

		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID_2, Status.OPPRETTET.toString(), MELDING, DISTRIBUSJON_ID);
		putMessageOnKafkaTopic(doknotifikasjonStatus);

		Notifikasjon updatedNotifikasjon = notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID);
		assertEquals(Status.FEILET.toString(), updatedNotifikasjon.getStatus().toString());
		assertNull(updatedNotifikasjon.getEndretAv());
		assertNull(updatedNotifikasjon.getEndretDato());
	}

 */

	private void putMessageOnKafkaTopic(DoknotifikasjonSms doknotifikasjonSms) {
		try {
			Long keyGenerator = System.currentTimeMillis();

			KafkaEventProducer.publish(
					KAFKA_TOPIC_DOK_NOTIFKASJON_SMS,
					doknotifikasjonSms,
					keyGenerator
			);

			TimeUnit.SECONDS.sleep(30);
		} catch (InterruptedException exception) {
			fail();
		}
	}

		/*
		@Primary
		@Bean
		public Jaxb2Marshaller marshaller() {
			Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
			marshaller.setContextPath("no.altinn.springsoap.client.gen");
			return marshaller;
		}

		@Primary
		@Bean
		public AltinnConsumer altinnConsumer(
				Jaxb2Marshaller marshaller
		) {


			AltinnConsumer client = new AltinnConsumer("username", "password");
			client.setDefaultUri("localhost");
			client.setMarshaller(marshaller);
			client.setUnmarshaller(marshaller);
			client.setWebServiceTemplate(mockWebServiceTemplate);

			//getWebServiceTemplate

			return client;
		}

		@Primary
		@Bean
		public NotifikasjonEntityMapper notifikasjonEntityMapper() {
			return new NotifikasjonEntityMapper(notifikasjonDistribusjonRepository);
		}

		@Primary
		@Bean
		public NotifikasjonDistribusjonConsumer notifikasjonDistribusjonConsumer(NotifikasjonEntityMapper notifikasjonEntityMapper){
			return new NotifikasjonDistribusjonConsumer(notifikasjonEntityMapper, KafkaEventProducer, altinnConsumer(marshaller()));
		}
	}
	*/
}