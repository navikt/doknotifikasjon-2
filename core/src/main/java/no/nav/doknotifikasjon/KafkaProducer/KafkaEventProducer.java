package no.nav.doknotifikasjon.KafkaProducer;


import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.technical.AuthenticationFailedException;
import no.nav.doknotifikasjon.exception.technical.KafkaTechnicalException;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

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
            if (log.isDebugEnabled()) {
                log.info("Published to partittion " + sendResult.getRecordMetadata().partition());
                log.info("Published to offset " + sendResult.getRecordMetadata().offset());
                log.info("Published to topic " + sendResult.getRecordMetadata().topic());
            }
        } catch (ExecutionException executionException) {
            if (executionException.getCause() != null && executionException.getCause() instanceof KafkaProducerException) {
                KafkaProducerException kafkaProducerException = (KafkaProducerException) executionException.getCause();
                if (kafkaProducerException.getCause() != null && kafkaProducerException.getCause() instanceof TopicAuthorizationException) {
                    throw new AuthenticationFailedException("Not authenticated to publish to topic '" + topic + "'", kafkaProducerException.getCause());
                }
            }
        } catch (InterruptedException e) {
            throw new KafkaTechnicalException("Failed to send message to kafka. Topic: " + topic, e);
        }
    }
}
