package no.nav.doknotifikasjon.KafkaEvents.producer;


import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
//@Component
public class EventProducer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void publish(
            String topic,
            String key,
            Object event,
            Long timestamp
    ) {
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(
                topic,
                null,
                timestamp,
                key,
                event
        );

        try {
            SendResult<String, Object> sendResult = kafkaTemplate.send(producerRecord).get();
            if(log.isDebugEnabled()) {
                log.info("Published to partittion " + sendResult.getRecordMetadata().partition());
                log.info("Published to offset " + sendResult.getRecordMetadata().offset());
                log.info("Published to offset " + sendResult.getRecordMetadata().topic());
            }
        }catch (Exception e) {
            System.out.println(e);
        }
//        catch (ExecutionException e) {
//            if(e.getCause() != null && e.getCause() instanceof KafkaProducerException) {
//                KafkaProducerException ee = (KafkaProducerException) e.getCause();
//                if(ee.getCause() != null && ee.getCause() instanceof TopicAuthorizationException) {
//                    throw new AuthenticationFailedExecption("Not authenticated to publish to topic '" + topic + "'", ee.getCause());
//                }
//            }
//        }
    }
}
