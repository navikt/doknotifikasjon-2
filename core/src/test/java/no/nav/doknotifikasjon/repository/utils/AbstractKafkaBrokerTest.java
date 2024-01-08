package no.nav.doknotifikasjon.repository.utils;

import jakarta.xml.bind.JAXBElement;
import no.nav.doknotifikasjon.config.cxf.BusConfig;
import no.nav.doknotifikasjon.repository.RepositoryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import javax.xml.namespace.QName;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(
		classes = {
				ApplicationTestConfig.class,
				RepositoryConfig.class,
				BusConfig.class},
		webEnvironment = RANDOM_PORT
)
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
