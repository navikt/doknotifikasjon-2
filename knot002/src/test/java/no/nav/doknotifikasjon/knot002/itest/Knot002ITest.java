package no.nav.doknotifikasjon.knot002.itest;


import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.EmbededKafkaBroker;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.client.core.WebServiceTemplate;

import java.util.concurrent.TimeUnit;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS;
import static org.junit.Assert.*;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.createNotifikasjonWithDistribusjon;
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



	}


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