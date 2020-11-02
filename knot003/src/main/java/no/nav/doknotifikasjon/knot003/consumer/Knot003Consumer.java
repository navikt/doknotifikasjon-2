package no.nav.doknotifikasjon.knot003.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.knot003.service.Knot003Service;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.doknotifikasjon.mdc.MDCGenerate.generateNewCallIdIfThereAreNone;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.setDistribusjonId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearCallId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearDistribusjonId;


@Slf4j
@Component
public class Knot003Consumer {

	private final ObjectMapper objectMapper;
	private final Knot003Service knot003Service;

	Knot003Consumer(
			Knot003Service knot003Service,
			ObjectMapper objectMapper
	) {
		this.knot003Service = knot003Service;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(
			topics = "privat-dok-notifikasjon-epost",
			containerFactory = "kafkaListenerContainerFactory",
			groupId = "doknotifikasjon-knot003"
	)
	@Transactional
	@Metrics(value = "dok_request", percentiles = {0.5, 0.95})
	public void onMessage(final ConsumerRecord<String, Object> record) {
		try {
			generateNewCallIdIfThereAreNone();

			DoknotifikasjonEpost doknotifikasjonEpost = objectMapper.readValue(record.value().toString(), DoknotifikasjonEpost.class);
			setDistribusjonId(String.valueOf(doknotifikasjonEpost.getNotifikasjonDistribusjonId()));

			log.info("knot003 starter behandling av NotifikasjonDistribusjonId={}", doknotifikasjonEpost.getNotifikasjonDistribusjonId());
			knot003Service.konsumerDistribusjonId(doknotifikasjonEpost.getNotifikasjonDistribusjonId());

		} catch (JsonProcessingException e) {
			log.error("Problemer med parsing av kafka-hendelse til Json. ", e);
		} catch (DoknotifikasjonValidationException e) {
			log.error("Valideringsfeil i knot003. Avslutter behandlingen. ", e);
		} catch (IllegalArgumentException e) {
			log.error("Valideringsfeil i knot003: Ugyldig status i hendelse på kafka-topic, avslutter behandlingen. ", e);
		} finally {
			clearDistribusjonId();
			clearCallId();
		}
	}
}
