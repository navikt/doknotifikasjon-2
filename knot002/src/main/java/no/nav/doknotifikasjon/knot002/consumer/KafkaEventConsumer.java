package no.nav.doknotifikasjon.knot002.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class KafkaEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotifikasjonDistribusjonConsumer notifikasjonDistribusjonConsumer;

    KafkaEventConsumer(
            NotifikasjonDistribusjonConsumer notifikasjonDistribusjonConsumer,
            ObjectMapper objectMapper
    ){
        this.notifikasjonDistribusjonConsumer = notifikasjonDistribusjonConsumer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "dok-eksternnotifikasjon-sms",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onMessage(final ConsumerRecord<String, Object> record) {
        try {
            DoknotifikasjonSms doknotifikasjonSms = objectMapper.readValue(record.value().toString(), DoknotifikasjonSms.class);

            notifikasjonDistribusjonConsumer.konsumerDistribusjonId(doknotifikasjonSms.getNotifikasjonDistribusjonId());
        } catch (JsonMappingException exception) {
            log.error("knot002 klarte ikke å mappe melding fra kø til objekt", exception);
        } catch (JsonProcessingException exception) {
            log.error("knot002 feilet i å prosessere melding fra kø", exception);
        }
    }
}
