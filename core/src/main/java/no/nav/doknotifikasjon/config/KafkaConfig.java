package no.nav.doknotifikasjon.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.backoff.FixedBackOff.DEFAULT_INTERVAL;
import static org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS;

@Slf4j
@EnableKafka
@Configuration
public class KafkaConfig {

	private KafkaProperties kafkaProperties;

	final String schemaUrl;
	final String basicAuth;

	@Inject
	public KafkaConfig(
			@Value("KAFKA_SCHEMA_REGISTRY") String kafka_schema_registry,
			@Value("KAFKA_SCHEMA_REGISTRY_USER") String KAFKA_SCHEMA_REGISTRY_USER,
			@Value("KAFKA_SCHEMA_REGISTRY_PASSWORD") String KAFKA_SCHEMA_REGISTRY_PASSWORD,
			KafkaProperties kafkaProperties
	) {
		this.schemaUrl = kafka_schema_registry;
		basicAuth = KAFKA_SCHEMA_REGISTRY_USER + ":" + KAFKA_SCHEMA_REGISTRY_PASSWORD;

		if (KAFKA_SCHEMA_REGISTRY_USER.isEmpty() || KAFKA_SCHEMA_REGISTRY_PASSWORD.isEmpty()) {
			log.warn("WARN til Joakim");
		}

		this.kafkaProperties = kafkaProperties;
	}

	@Bean("kafkaListenerContainerFactory")
	@Primary
	ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerFactory(
			ConsumerFactory<Object, Object> kafkaConsumerFactory
	) {
		ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(kafkaConsumerFactory);
		factory.getContainerProperties()
				.setAuthorizationExceptionRetryInterval(Duration.ofSeconds(10L));

		factory.setConcurrency(3);
		factory.setErrorHandler(new SeekToCurrentErrorHandler(
				(rec, thr) -> log.error("Exception oppstått i doknotifikasjon={} kafka record til topic={}, partition={}, offset={}, UUID={} feilmelding={}",
						thr.getClass().getSimpleName(),
						rec.topic(),
						rec.partition(),
						rec.offset(),
						rec.key(),
						thr.getCause()
				),
				new FixedBackOff(DEFAULT_INTERVAL, UNLIMITED_ATTEMPTS)));
		return factory;
	}

	@Bean
	public Map<String, Object> kafkaProperties() {
		Map<String, Object> configProps =
				new HashMap<>(kafkaProperties.buildProducerProperties());
		configProps.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaUrl);
		configProps.put(KafkaAvroDeserializerConfig.USER_INFO_CONFIG, basicAuth);
		configProps.put(KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
		return configProps;
	}
}
