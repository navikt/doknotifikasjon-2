package no.nav.doknotifikasjon.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.metrics.MetricService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import javax.inject.Inject;

@Slf4j
@EnableKafka
@Configuration
public class KafkaConfig {

	@Bean("kafkaListenerContainerFactory")
	@Primary
	ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerFactory(
			ConsumerFactory<Object, Object> kafkaConsumerFactory
	) {
		ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(kafkaConsumerFactory);

		factory.setConcurrency(3);
		factory.setErrorHandler(new SeekToCurrentErrorHandler(
				(rec, thr) -> log.error("Fikk exception: {} kafka record til topic: {}, partition: {}, offset: {}, UUID: {} feilmelding={}",
						thr.getClass().getSimpleName(),
						rec.topic(),
						rec.partition(),
						rec.offset(),
						rec.key(),
						thr.getCause()
				),
				new FixedBackOff(FixedBackOff.DEFAULT_INTERVAL, 0)));
		return factory;
	}
}
