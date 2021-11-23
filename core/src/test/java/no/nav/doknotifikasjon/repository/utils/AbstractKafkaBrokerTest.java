package no.nav.doknotifikasjon.repository.utils;

import no.nav.doknotifikasjon.repository.RepositoryConfig;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

@SpringBootTest(
		classes = {ApplicationTestConfig.class, RepositoryConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.main.allow-bean-definition-overriding=true"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext()
@ActiveProfiles({"itest", "itestKafka"})
@EmbeddedKafka(
		topics = {
				"privat-dok-notifikasjon",
				"privat-dok-notifikasjon-med-kontakt-info",
				"aapen-dok-notifikasjon-status",
				"privat-dok-notifikasjon-sms",
				"privat-dok-notifikasjon-epost"
		},
		bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@AutoConfigureWireMock(port = 0)
public abstract class AbstractKafkaBrokerTest {

	@Autowired
	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	public EmbeddedKafkaBroker kafkaEmbedded;

	protected JAXBElement<String> constructJaxbElement(String local, String value) {
		return new JAXBElement<>(new QName("http://www.altinn.no/services/common/fault/2009/10", local), String.class, value);
	}
}
