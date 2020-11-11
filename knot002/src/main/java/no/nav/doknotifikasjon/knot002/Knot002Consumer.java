package no.nav.doknotifikasjon.knot002;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearCallId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearDistribusjonId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.generateNewCallIdIfThereAreNone;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.setDistribusjonId;


@Slf4j
@Component
public class Knot002Consumer {

    private final ObjectMapper objectMapper;
    private final Knot002Service knot002Service;

    @Inject
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
    @Metrics(value = "dok_request", percentiles = {0.5, 0.95})
    public void onMessage(final ConsumerRecord<String, Object> record) {
        try {
            generateNewCallIdIfThereAreNone();

            DoknotifikasjonSms doknotifikasjonSms = objectMapper.readValue(record.value().toString(), DoknotifikasjonSms.class);
            setDistribusjonId(String.valueOf(doknotifikasjonSms.getNotifikasjonDistribusjonId()));

            knot002Service.shouldSendSms(doknotifikasjonSms.getNotifikasjonDistribusjonId());

        } catch (JsonProcessingException e) {
            log.error("Problemer med parsing av kafka-hendelse til Json. ", e);
        } catch (DoknotifikasjonDistribusjonIkkeFunnetException e) {
            log.error("Ingen notifikasjonDistribusjon ble funnet i databasen. Avslutter behandlingen. ", e);
        } catch (DoknotifikasjonValidationException e) {
            log.error("Valideringsfeil i knot002. Avslutter behandlingen. ", e);
        } catch (Exception e) {
            log.error("Knot002 feilet pga ukjent feil. Behandlingen avsluttes.", e);
        } finally {
            clearDistribusjonId();
            clearCallId();
        }
    }
}
