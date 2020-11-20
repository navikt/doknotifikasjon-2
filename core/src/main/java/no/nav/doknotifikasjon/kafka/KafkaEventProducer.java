package no.nav.doknotifikasjon.kafka;


import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.technical.AuthenticationFailedException;
import no.nav.doknotifikasjon.exception.technical.KafkaTechnicalException;
import no.nav.doknotifikasjon.metrics.Metrics;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static no.nav.doknotifikasjon.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;
import static no.nav.doknotifikasjon.constants.RetryConstants.MAX_INT;

@Slf4j
@Component
public class KafkaEventProducer {

	private static final String KAFKA_NOT_AUTHENTICATED = "Not authenticated to publish to topic: ";
	private static final String KAFKA_FAILED_TO_SEND = "Failed to send message to kafka. Topic: ";

	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Inject
	KafkaEventProducer(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") KafkaTemplate<String, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@Metrics(createErrorMetric = true, errorMetricInclude = KafkaTechnicalException.class)
	@Retryable(include = KafkaTechnicalException.class, maxAttempts = MAX_INT, backoff = @Backoff(delay = DELAY_LONG))
	public void publish(String topic, Object event) {
		this.publish(
				topic,
				event,
				System.currentTimeMillis()
		);
	}

	@Transactional
	void publish(
			String topic,
			Object event,
			Long timestamp
	) {
		ProducerRecord<String, Object> producerRecord = new ProducerRecord(
				topic,
				null,
				timestamp,
				this.getDefaultUuidIfNoCallIdIsSett(),
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
			if (executionException.getCause() instanceof KafkaProducerException) {
				KafkaProducerException kafkaProducerException = (KafkaProducerException) executionException.getCause();
				if (kafkaProducerException.getCause() instanceof TopicAuthorizationException) {
					throw new AuthenticationFailedException(KAFKA_NOT_AUTHENTICATED + topic, kafkaProducerException.getCause());
				}
			}
			throw new KafkaTechnicalException(KAFKA_FAILED_TO_SEND + topic, executionException);
		} catch (InterruptedException e) {
			throw new KafkaTechnicalException(KAFKA_FAILED_TO_SEND + topic, e);
		}
	}

	private String getDefaultUuidIfNoCallIdIsSett() {
		if (MDC.get(MDC_CALL_ID) != null && !MDC.get(MDC_CALL_ID).trim().isEmpty()) {
			return MDC.get(MDC_CALL_ID);
		}
		return UUID.randomUUID().toString();
	}
}
