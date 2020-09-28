package no.nav.doknotifikasjon.consumer;

import no.nav.doknotifikasjon.producer.KafkaEventProducer;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
//import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration

import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_EKSTERN_NOTIFKASJON;

//@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
//        topics = "[OMP_UTBETALING_ARBEIDSTAKER]",
//        bootstrapServersProperty = "spring.kafka.bootstrap-servers" // Setter bootstrap-servers for consumer og producer.
//)
//@ExtendWith(SpringExtension.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@DirtiesContext // Forsikrer at riktig kafka broker addresse blir satt for testen.
//@ActiveProfiles("test")
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
////@Import(TokenGeneratorConfiguration.class) // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
//@AutoConfigureWireMock(port = 8000) // Konfigurerer og setter opp en wiremockServer. Default leses src/test/resources/__files og src/test/resources/mappings
public class Test extends AbstractIT {

    @Autowired
    KafkaEventConsumer kafkaEventConsumer;

    @Autowired
    KafkaEventProducer KafkaEventProducer;

    @org.junit.Test
    public void test() {
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

        KafkaEventProducer.publish(
                KAFKA_TOPIC_DOK_EKSTERN_NOTIFKASJON,
                keyGenerator.toString(),
                dokEksternNotifikasjon,
                keyGenerator
        );
    }

}
