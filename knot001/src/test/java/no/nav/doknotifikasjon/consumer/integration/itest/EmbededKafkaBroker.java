package no.nav.doknotifikasjon.consumer.integration.itest;

import no.nav.doknotifikasjon.consumer.integration.config.ApplicationTestConfig;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = {ApplicationTestConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext()
@ActiveProfiles("itest")
@EmbeddedKafka(
        topics = {
            "test_topic",
            "privat-dok-notifikasjon"
        },
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
public class EmbededKafkaBroker {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public EmbeddedKafkaBroker kafkaEmbedded;
}
