package no.nav.doknotifikasjon.KafkaEvents.producer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.producer.KafkaEventProducer;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_EKSTERN_NOTIFKASJON;

@Slf4j
@RestController
@RequestMapping("/kafka")
public class ProducerController {

    private static final String KAFKA_PRODUCER_TEST_WORK = "Kafka mannaged to produce";

    @Autowired
    private KafkaEventProducer publisher;

    @GetMapping("/test")
    public String kafkaProduceMessage() {
        Doknotifikasjon dokEksternNotifikasjon = new Doknotifikasjon(
                "bestillingsId",
                "bestillerId",
                "fodselsnummer",
                0,
                0,
                "tittel",
                "tekst",
                "prefererteKanaler"
        );

        Long keyGenerator = System.currentTimeMillis();

        publisher.publish(
                KAFKA_TOPIC_DOK_EKSTERN_NOTIFKASJON,
                keyGenerator.toString(),
                dokEksternNotifikasjon,
                keyGenerator
        );

        return KAFKA_PRODUCER_TEST_WORK;
    }
}