package no.nav.doknotifikasjon.itest;

import no.nav.doknotifikasjon.config.ApplicationTestConfig;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {ApplicationTestConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext()
@ActiveProfiles("itest")
@EmbeddedKafka(
        topics = {
            "test_topic",
            "privat-dok-notifikasjon-status"
        },
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
public class EmbededKafkaBroker {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public EmbeddedKafkaBroker kafkaEmbedded;
}
