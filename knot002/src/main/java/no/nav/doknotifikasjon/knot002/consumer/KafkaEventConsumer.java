package no.nav.doknotifikasjon.knot002.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
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

    private final NotifikasjonDistribusjonIdConsumer notifikasjonDistribusjonIdConsumer;

    KafkaEventConsumer(NotifikasjonDistribusjonIdConsumer notifikasjonDistribusjonIdConsumer){
        this.notifikasjonDistribusjonIdConsumer = notifikasjonDistribusjonIdConsumer;
    }

    @KafkaListener(
            topics = "dok-eksternnotifikasjon-sms",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onMessage(final ConsumerRecord<String, Object> record) {
        try {
            DoknotifikasjonSms doknotifikasjonSms = objectMapper.readValue(record.value().toString(), DoknotifikasjonSms.class);

            notifikasjonDistribusjonIdConsumer.konsumerDistribusjonId(doknotifikasjonSms.getNotifikasjonDistribusjonId());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }
}
