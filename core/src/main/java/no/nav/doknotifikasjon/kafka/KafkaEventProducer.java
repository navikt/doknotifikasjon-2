package no.nav.doknotifikasjon.kafka;


import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.technical.KafkaTechnicalException;
import no.nav.doknotifikasjon.metrics.Metrics;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;
import static no.nav.doknotifikasjon.constants.RetryConstants.RETRIES;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.getDefaultUuidIfNoCallIdIsSett;

@Slf4j
@Component
public class KafkaEventProducer {

	private static final String KAFKA_NOT_AUTHENTICATED = "Not authenticated to publish to topic: ";
	private static final String KAFKA_FAILED_TO_SEND = "Failed to send message to kafka. Topic: ";

	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	KafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@Metrics(createErrorMetric = true, errorMetricInclude = KafkaTechnicalException.class)
	@Retryable(include = KafkaTechnicalException.class, maxAttempts = RETRIES, backoff = @Backoff(delay = DELAY_LONG))
	public void publish(String topic, Object event) {
		this.publish(
				topic,
				event,
				getDefaultUuidIfNoCallIdIsSett()
		);
	}

	@Metrics(createErrorMetric = true, errorMetricInclude = KafkaTechnicalException.class)
	@Retryable(include = KafkaTechnicalException.class, maxAttempts = RETRIES, backoff = @Backoff(delay = DELAY_LONG))
	public void publishWithKey(String topic, Object event, String keyValue) {
		this.publish(
				topic,
				event,
				keyValue
		);
	}

	void publish(
			String topic,
			Object event,
			String keyValue
	) {
		ProducerRecord<String, Object> producerRecord = new ProducerRecord(
				topic,
				null,
				System.currentTimeMillis(),
				keyValue,
				event
		);

		try {
			SendResult<String, Object> sendResult = kafkaTemplate.send(producerRecord).get();
			log.info("Message stored on topic. Timestamp={}, partition={}, offset={}, topic={}",
					sendResult.getRecordMetadata().timestamp(),
					sendResult.getRecordMetadata().partition(),
					sendResult.getRecordMetadata().offset(),
					sendResult.getRecordMetadata().topic()
			);
		} catch (ExecutionException executionException) {
			if (executionException.getCause() instanceof KafkaProducerException) {
				KafkaProducerException kafkaProducerException = (KafkaProducerException) executionException.getCause();
				if (kafkaProducerException.getCause() instanceof TopicAuthorizationException) {
					throw new KafkaTechnicalException(KAFKA_NOT_AUTHENTICATED + topic, kafkaProducerException.getCause());
				}
			}
			throw new KafkaTechnicalException(KAFKA_FAILED_TO_SEND + topic, executionException);
		} catch (InterruptedException | KafkaException e) {
			throw new KafkaTechnicalException(KAFKA_FAILED_TO_SEND + topic, e);
		}
	}
}
