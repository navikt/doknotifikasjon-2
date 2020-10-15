package no.nav.doknotifikasjon;

import static no.nav.doknotifikasjon.TestUtils.BESTILLER_ID;
import static no.nav.doknotifikasjon.TestUtils.BESTILLING_ID;
import static no.nav.doknotifikasjon.TestUtils.MELDING;
import static no.nav.doknotifikasjon.TestUtils.STATUS;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON;
import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.doknotifikasjon.KafkaProducer.KafkaEventProducer;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;


public class Knot004Itest  {

//	@Autowired
//	private NotifikasjonRepository notifikasjonRepository;
//
//	@Autowired
//	private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

//	@Autowired
//	private KafkaEventProducer KafkaEventProducer;

//	@BeforeEach
//	public void setup() {
//		notifikasjonRepository.deleteAll();
//		notifikasjonDistribusjonRepository.deleteAll();
//	}

//	@Test
//	public void shouldUpdateStatus() throws InterruptedException{
////		Notifikasjon notifikasjon = createNotifikasjon();
////		notifikasjonRepository.saveAndFlush(notifikasjon);
//
//		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLING_ID, BESTILLER_ID, STATUS, MELDING, null);
//		Long keyGenerator = System.currentTimeMillis();
//
//		KafkaEventProducer.publish(
//				"privat-dok-notifikasjon-status",
//				doknotifikasjonStatus,
//				keyGenerator
//		);
//		TimeUnit.SECONDS.sleep(5);
//
////		notifikasjon.getStatus();
//	}
}

