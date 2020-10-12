package no.nav.doknotifikasjon.consumer;


import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.metrics.Metrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
public class KafkaEventConsumer {

    @KafkaListener(
            topics = "privat-dok-ekstern-notifikasjon-status",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Metrics(value = "dok_request", percentiles = {0.5, 0.95})  //todo: Lage metrics? Grafana?
    @Transactional
    public void onMessage(final ConsumerRecord<?, ?> record) {
        MDC.put("callId", UUID.randomUUID().toString());        //todo: Skal denne være her?

//        System.out.println("record incoming for consumer: " + record);
//        log.warn("record incoming for consumer: " + record);
    }
}
