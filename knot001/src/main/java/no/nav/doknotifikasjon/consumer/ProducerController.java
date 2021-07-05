package no.nav.doknotifikasjon.consumer;


import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.schemas.PrefererteKanal;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON;


@Slf4j
@RestController
@RequestMapping("/kafka")
public class ProducerController {

	private final KafkaEventProducer publisher;
	final String schemaUrl;
	final String basicAuth;
	final String kafkaBrokers;
	final String kafkaTrustorePath;
	final String kafkaKeystorePath;
	final String kafkaCredstorePassword;

	ProducerController(
			KafkaEventProducer publisher,
			@Value("KAFKA_SCHEMA_REGISTRY") String kafka_schema_registry,
			@Value("KAFKA_SCHEMA_REGISTRY_USER") String KAFKA_SCHEMA_REGISTRY_USER,
			@Value("KAFKA_SCHEMA_REGISTRY_PASSWORD") String KAFKA_SCHEMA_REGISTRY_PASSWORD,
			@Value("KAFKA_BROKERS") String KAFKA_BROKERS,
			@Value("KAFKA_TRUSTSTORE_PATH") String KAFKA_TRUSTSTORE_PATH,
			@Value("KAFKA_KEYSTORE_PATH") String KAFKA_KEYSTORE_PATH,
			@Value("KAFKA_CREDSTORE_PASSWORD") String KAFKA_CREDSTORE_PASSWORD
	) {
		this.publisher = publisher;

		this.schemaUrl = kafka_schema_registry;
		this.basicAuth = KAFKA_SCHEMA_REGISTRY_USER + ":" + KAFKA_SCHEMA_REGISTRY_PASSWORD;
		this.kafkaBrokers = KAFKA_BROKERS;
		this.kafkaTrustorePath = KAFKA_TRUSTSTORE_PATH;
		this.kafkaKeystorePath = KAFKA_KEYSTORE_PATH;
		this.kafkaCredstorePassword = KAFKA_CREDSTORE_PASSWORD;
	}

	//This code should not be in prod!
	@GetMapping("/test")
	public void kafkaProduceMessage() {
		List<PrefererteKanal> preferteKanaler = List.of(PrefererteKanal.EPOST, PrefererteKanal.SMS);

		Doknotifikasjon dokEksternNotifikasjon = new Doknotifikasjon(
				LocalDateTime.now().toString(),
				LocalDateTime.now().toString(),
				0,
				"09097400366", // FNR er fra en testbrukker hos dolly
				0,
				0,
				"TITTEL",
				"epostTekst",
				"smsTekst",
				preferteKanaler
		);

//		publisher.publish(
//				KAFKA_TOPIC_DOK_NOTIFKASJON,
//				dokEksternNotifikasjon
//		);





		final Properties props = new Properties();
		props.put("bootstrap.servers", kafkaBrokers + ":localhost:9092");

		props.put("security.protocol", "SSL");
		props.put("ssl.keystore.type", "PKCS12");
		props.put("ssl.truststore.type", "JKS");

		props.put("ssl.truststore.location", kafkaTrustorePath);
		props.put("ssl.keystore.location", kafkaKeystorePath);
		props.put("ssl.truststore.password", kafkaCredstorePassword);
		props.put("ssl.keystore.password", kafkaCredstorePassword);
		props.put("ssl.key.password", kafkaCredstorePassword);

		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
		props.put("schema.registry.url", schemaUrl);
		props.put("basic.auth.credentials.source", "USER_INFO");
		props.put("basic.auth.user.info", basicAuth);
		KafkaProducer producer = new KafkaProducer(props);
		try {
			producer = new KafkaProducer(props);
			producer.send(new ProducerRecord("topic1", dokEksternNotifikasjon));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			producer.close();
		}
	}
}