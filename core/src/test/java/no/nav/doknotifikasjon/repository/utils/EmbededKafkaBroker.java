package no.nav.doknotifikasjon.repository.utils;

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
@ActiveProfiles("itestKafka")
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
