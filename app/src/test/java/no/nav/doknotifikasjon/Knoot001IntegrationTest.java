package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.KafkaProducer.KafkaEventProducer;
import no.nav.doknotifikasjon.itest.EmbededKafkaBroker;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON;

@SpringBootTest
public class Knoot001IntegrationTest extends EmbededKafkaBroker {

    @Autowired
    KafkaEventProducer KafkaEventProducer;

    @Test
    public void TestPosetivConsumer() throws InterruptedException {
        Doknotifikasjon dokEksternNotifikasjon = new Doknotifikasjon(
                "bestillingsId",
                "bestillerId",
                "fodselsnummer",
                0,
                0,
                "tittel",
                "epostTekst",
                "smsTekst",
                "prefererteKanaler"
        );

        Long keyGenerator = System.currentTimeMillis();

        KafkaEventProducer.publish(
                KAFKA_TOPIC_DOK_NOTIFKASJON,
                dokEksternNotifikasjon,
                keyGenerator
        );
        TimeUnit.SECONDS.sleep(5);
    }
}
