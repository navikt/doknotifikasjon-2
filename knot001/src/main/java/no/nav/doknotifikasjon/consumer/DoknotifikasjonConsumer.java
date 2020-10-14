package no.nav.doknotifikasjon.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.KafkaProducer.KafkaDoknotifikasjonStatusProducer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaException;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.service.NotifikasjonService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.doknotifikasjon.KafkaProducer.DoknotifikasjonStatusMessage.OVERSENDT_NOTIFIKASJON_PROCESSED;
import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS;

@Slf4j
@Component
public class DoknotifikasjonConsumer {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    NotifikasjonService notifikasjonService;

    @Autowired
    DoknotifikasjonService doknotifikasjonService;

    @Autowired
    KafkaDoknotifikasjonStatusProducer StatusProducer;

    @Autowired
    DigitalKontaktinfoConsumer kontaktinfoConsumer;

    @KafkaListener(
            topics = "privat-dok-notifikasjon",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onMessage(final ConsumerRecord<String, Object> record) {
        Doknotifikasjon doknotifikasjon;

        try {
            doknotifikasjon = objectMapper.readValue(record.value().toString(), Doknotifikasjon.class);
        } catch (JsonProcessingException e) {
            throw new InvalidAvroSchemaException("Kafka event does not have the required vale of object Doknotifikasjon");
        }

        doknotifikasjonService.validateAvroDoknotifikasjon(doknotifikasjon);

        // DKIF
        DigitalKontaktinformasjonTo.DigitalKontaktinfo digitalKontaktinfo = kontaktinfoConsumer.hentDigitalKontaktinfo(
                doknotifikasjon.getFodselsnummer()
        );

        doknotifikasjonService.createNotifikasjonFromDoknotifikasjon(doknotifikasjon, digitalKontaktinfo);

        doknotifikasjonService.publishDoknotikfikasjonSms(doknotifikasjon.getBestillingsId());
        doknotifikasjonService.publishDoknotikfikasjonEpost(doknotifikasjon.getBestillingsId());

        StatusProducer.publishDoknotikfikasjonStatusOversendt(
                KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
                doknotifikasjon.getBestillingsId(),
                doknotifikasjon.getBestillerId(),
                OVERSENDT_NOTIFIKASJON_PROCESSED,
                null
        );
    }
}
