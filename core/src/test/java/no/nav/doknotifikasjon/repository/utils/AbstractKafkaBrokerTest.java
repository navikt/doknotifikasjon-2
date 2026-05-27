package no.nav.doknotifikasjon.repository.utils;

import no.nav.doknotifikasjon.repository.RepositoryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.ConfigureWireMock;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(
		classes = {ApplicationTestConfig.class, RepositoryConfig.class},
		webEnvironment = RANDOM_PORT
)
@ActiveProfiles({"itest", "itestKafka"})
@EmbeddedKafka(
		partitions = 1,
		topics = {
				"teamdokumenthandtering.privat-dok-notifikasjon",
				"teamdokumenthandtering.privat-dok-notifikasjon-med-kontakt-info",
				"teamdokumenthandtering.aapen-dok-notifikasjon-status",
				"teamdokumenthandtering.privat-dok-notifikasjon-sms",
				"teamdokumenthandtering.privat-dok-notifikasjon-epost",
				"teamdokumenthandtering.privat-dok-notifikasjon-stopp"
		}
)
@ConfigureWireMock
public abstract class AbstractKafkaBrokerTest {

	@Autowired
	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	public EmbeddedKafkaBroker kafkaEmbedded;

}
