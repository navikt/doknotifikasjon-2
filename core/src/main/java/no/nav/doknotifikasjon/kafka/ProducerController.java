package no.nav.doknotifikasjon.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON;

@Slf4j
@RestController
@RequestMapping("/kafka")
public class ProducerController {

    @Autowired
    private KafkaEventProducer publisher;

    @GetMapping("/test")
    public void kafkaProduceMessage() {
        Doknotifikasjon dokEksternNotifikasjon = new Doknotifikasjon(
                "bestillingsId",
                "bestillerId",
                "24117423766",
                0,
                0,
                "tittel",
                "epostTekst",
                "smsTekst",
                "prefererteKanaler"
        );

        Long keyGenerator = System.currentTimeMillis();

        publisher.publish(
                KAFKA_TOPIC_DOK_NOTIFKASJON,
                dokEksternNotifikasjon,
                keyGenerator
        );
    }
}