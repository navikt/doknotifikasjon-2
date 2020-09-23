package no.nav.doknotifikasjon.producer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.domain.DokEksternNotifikasjon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
        DokEksternNotifikasjon dokEksternNotifikasjon = new DokEksternNotifikasjon(
                "bestillingsId",
                "bestillerId",
                "fodselsnummer",
                0,
                0,
                "tittel",
                "tekst",
                "prefererteKanaler"
        );

        publisher.publish(
                KAFKA_TOPIC_DOK_EKSTERN_NOTIFKASJON,
                "String key",
                dokEksternNotifikasjon,
                null
        );

        return KAFKA_PRODUCER_TEST_WORK;
    }
}