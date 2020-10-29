package no.nav.doknotifikasjon.knot002.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.knot002.service.Knot002Service;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class Knot002Consumer {

    private final ObjectMapper objectMapper;
    private final Knot002Service knot002Service;

    Knot002Consumer(
            Knot002Service knot002Service,
            ObjectMapper objectMapper
    ) {
        this.knot002Service = knot002Service;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "privat-dok-notifikasjon-sms",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "doknotifikasjon-knot002"
    )
    @Transactional
    public void onMessage(final ConsumerRecord<String, Object> record) {
        try {
            DoknotifikasjonSms doknotifikasjonSms = objectMapper.readValue(record.value().toString(), DoknotifikasjonSms.class);
            log.info("knot002 starter behandling av NotifikasjonDistribusjonId={}", doknotifikasjonSms.getNotifikasjonDistribusjonId());
            knot002Service.konsumerDistribusjonId(doknotifikasjonSms.getNotifikasjonDistribusjonId());
        } catch (JsonProcessingException e) {
            log.error("Problemer med parsing av kafka-hendelse til Json. ", e);
        } catch (DoknotifikasjonValidationException e) {
            log.error("Valideringsfeil i knot002. Avslutter behandlingen. ", e);
        } catch (IllegalArgumentException e) {
            log.error("Valideringsfeil i knot002: Ugyldig status i hendelse på kafka-topic, avslutter behandlingen. ", e);
        }
    }
}
