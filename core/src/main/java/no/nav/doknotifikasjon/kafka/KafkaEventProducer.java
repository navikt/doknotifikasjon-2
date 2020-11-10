package no.nav.doknotifikasjon.kafka;


import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.technical.AuthenticationFailedException;
import no.nav.doknotifikasjon.exception.technical.KafkaTechnicalException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class KafkaEventProducer {

	private static String KAFKA_NOT_AUTHENTICATED = "Not authenticated to publish to topic: ";
	private static String KAFKA_FAILED_TO_SEND = "Failed to send message to kafka. Topic: ";

	private final KafkaTemplate<String, Object> kafkaTemplate;

	KafkaEventProducer(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") KafkaTemplate<String, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void publish(String topic, Object event) {
		this.publish(
				topic,
				UUID.randomUUID().toString(),
				event,
				System.currentTimeMillis()
		);
	}


	public void publish(String topic, Object event, Long timestamp) {
		this.publish(
				topic,
				UUID.randomUUID().toString(),
				event,
				timestamp
		);
	}

	@Transactional
	public void publish(
			String topic,
			String key,
			Object event,
			Long timestamp
	) {
		ProducerRecord<String, Object> producerRecord = new ProducerRecord(
				topic,
				null,
				timestamp,
				key,
				event
		);

		try {
			SendResult<String, Object> sendResult = kafkaTemplate.send(producerRecord).get();
			log.info("Message stored on topic. Timestamp: {}, partition: {}, offset: {}, topic: {}",
					timestamp,
					sendResult.getRecordMetadata().partition(),
					sendResult.getRecordMetadata().offset(),
					sendResult.getRecordMetadata().topic()
			);
		} catch (ExecutionException executionException) {
			if (executionException.getCause() != null && executionException.getCause() instanceof KafkaProducerException) {
				KafkaProducerException kafkaProducerException = (KafkaProducerException) executionException.getCause();
				if (kafkaProducerException.getCause() != null && kafkaProducerException.getCause() instanceof TopicAuthorizationException) {
					throw new AuthenticationFailedException(KAFKA_NOT_AUTHENTICATED + topic, kafkaProducerException.getCause());
				}
			}
			throw new KafkaTechnicalException(KAFKA_FAILED_TO_SEND + topic, executionException);
		} catch (InterruptedException e) {
			throw new KafkaTechnicalException(KAFKA_FAILED_TO_SEND + topic, e);
		}
	}
}
