//package no.nav.doknotifikasjon.consumer;
//
//import org.junit.jupiter.api.TestInstance;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//
//
//@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
//        topics = "KAFKA_TOPIC_DOK_EKSTERN_NOTIFKASJON", // TODO import constant
//        bootstrapServersProperty = "spring.kafka.bootstrap-servers" // Setter bootstrap-servers for consumer og producer.
//)
//@ExtendWith(SpringExtension.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@DirtiesContext // Forsikrer at riktig kafka broker addresse blir satt for testen.
//@ActiveProfiles("test")
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
////@Import(TokenGeneratorConfiguration.class) // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
////@AutoConfigureWireMock(port = 8000) // Konfigurerer og setter opp en wiremockServer. Default leses src/test/resources/__files og src/test/resources/mappings
//class KafkaEventConsumerTest {
//
//
//}
