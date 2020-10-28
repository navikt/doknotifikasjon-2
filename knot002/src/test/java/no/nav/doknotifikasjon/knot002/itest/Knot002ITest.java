package no.nav.doknotifikasjon.knot002.itest;


import no.altinn.springsoap.client.gen.TransportType;
import no.nav.doknotifikasjon.consumer.altinn.AltinnConsumer;
import no.nav.doknotifikasjon.consumer.altinn.AltinnTestConfig;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.knot002.mapper.NotifikasjonEntityMapper;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.EmbededKafkaBroker;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

import java.util.concurrent.TimeUnit;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.generateAltinnResponse;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.KONTAKTINFO;
import static org.junit.Assert.*;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.createNotifikasjonWithDistribusjon;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.NOTIFIKASJONDISTRIBUSJONID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Knot002ITest extends EmbededKafkaBroker {




	@Autowired
	private KafkaEventProducer KafkaEventProducer;

	@Autowired
	private NotifikasjonRepository notifikasjonRepository;

	@Autowired
	private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@SpyBean
	private NotifikasjonEntityMapper NotifikasjonEntityMapper;

	@BeforeEach
	public void setup() {
			notifikasjonDistribusjonRepository.deleteAll();
			notifikasjonRepository.deleteAll();
			reset(AltinnTestConfig.getWebServiceTemplateMock());
	}

	@Test
	void shouldSetFerdigstilltStatusOnHappyPath() {

		WebServiceTemplate webServiceTemplateMock = AltinnTestConfig.getWebServiceTemplateMock();

		when(webServiceTemplateMock.marshalSendAndReceive(any())).thenReturn(generateAltinnResponse(TransportType.SMS, KONTAKTINFO));


		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), Status.OPPRETTET));

		Integer integerId = notifikasjonDistribusjon.getId();
		String stringId = Integer.toString(integerId);

		DoknotifikasjonSms doknotifikasjonSms= new DoknotifikasjonSms(stringId);
		putMessageOnKafkaTopic(doknotifikasjonSms);

		NotifikasjonDistribusjon updatedNotifikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(integerId).orElseThrow(()->new RuntimeException("Failed test!"));


		assertEquals(Status.FERDIGSTILT, updatedNotifikasjonDistribusjon.getStatus());
		assertEquals(Kanal.SMS, updatedNotifikasjonDistribusjon.getKanal());
		assertEquals("teamdokumenthandtering", updatedNotifikasjonDistribusjon.getEndretAv());
		assertNotNull(updatedNotifikasjonDistribusjon.getEndretDato());
	}

	@Test
	void shouldAbortIfDistribusjonNotFound() {

		DoknotifikasjonSms doknotifikasjonSms= new DoknotifikasjonSms("1234");
		putMessageOnKafkaTopic(doknotifikasjonSms);

		NotifikasjonDistribusjon updatedNotifikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(1234).orElse(null);

		assertNull(updatedNotifikasjonDistribusjon);
		try{
			verify(NotifikasjonEntityMapper, times(3)).mapNotifikasjon("1234");
			verify(NotifikasjonEntityMapper, times(1)).recoverMapNotifikasjon(any(), eq("1234"));
		} catch(Exception e){
			fail();
		}
	}



	private void putMessageOnKafkaTopic(DoknotifikasjonSms doknotifikasjonSms) {
		try {
			Long keyGenerator = System.currentTimeMillis();

			KafkaEventProducer.publish(
					KAFKA_TOPIC_DOK_NOTIFKASJON_SMS,
					doknotifikasjonSms,
					keyGenerator
			);

			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException exception) {
			fail();
		}
	}
}