package no.nav.doknotifikasjon.consumer;


import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class KafkaEventConsumer {

    @KafkaListener(topics = "aapen-dok-ekstern-notifikasjon") // TODO create enum/constant
    @Transactional
    public void onMessage(final ConsumerRecord<?, ?> record) {
        System.out.println("record: " + record);
    }
}
