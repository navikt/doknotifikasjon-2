//package no.nav.doknotifikasjon;
//
//import no.nav.doknotifikasjon.producer.KafkaEventProducer;
//import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
//import org.junit.Test;
//import org.mockito.Mock;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.transaction.annotation.Transactional;
//
//import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_EKSTERN_NOTIFKASJON;
//
//@SpringBootTest
//public class Testen extends AbstractIT {
//
//    @Mock
//    private KafkaTemplate<String, Object> kafkaTemplate;
//
//    @Autowired
//    KafkaEventProducer KafkaEventProducer;
//
//    @Test
//    @Transactional
//    public void testen() {
//        Doknotifikasjon dokEksternNotifikasjon = new Doknotifikasjon(
//                "bestillingsId",
//                "bestillerId",
//                "fodselsnummer",
//                0,
//                0,
//                "tittel",
//                "tekst",
//                "prefererteKanaler"
//        );
//
//        Long keyGenerator = System.currentTimeMillis();
//
//        KafkaEventProducer.publish(
//                KAFKA_TOPIC_DOK_EKSTERN_NOTIFKASJON,
//                keyGenerator.toString(),
//                dokEksternNotifikasjon,
//                keyGenerator
//        );
//    }
//
//}
