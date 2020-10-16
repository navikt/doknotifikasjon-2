package no.nav.doknotifikasjon.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class KafkaEventConsumer {

    @Autowired
    ObjectMapper objectMapper;

    @KafkaListener(
            topics = "privat-dok-notifikasjon",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onMessage(final ConsumerRecord<String, Object> record) {
        Doknotifikasjon doknotifikasjon = null;

        try {
            doknotifikasjon = objectMapper.readValue(record.value().toString(), Doknotifikasjon.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        doknotifikasjon.getAntallRenotifikasjoner();
    }
}