package no.nav.doknotifikasjon.knot003;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearCallId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearDistribusjonId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.generateNewCallIdIfThereAreNone;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.setDistribusjonId;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT003_CONSUMER;


@Slf4j
@Component
public class Knot003Consumer {

	private final ObjectMapper objectMapper;
	private final Knot003Service knot003Service;

	@Inject
	Knot003Consumer(Knot003Service knot003Service, ObjectMapper objectMapper) {
		this.knot003Service = knot003Service;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(
			topics = KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST,
			containerFactory = "kafkaListenerContainerFactory",
			groupId = "doknotifikasjon-knot003"
	)
	@Transactional
	@Metrics(value = DOK_KNOT003_CONSUMER, percentiles = {0.5, 0.95})
	public void onMessage(final ConsumerRecord<String, Object> record) {
		try {
			log.info("Innkommende kafka record til topic: {}, partition: {}, offset: {}", record.topic(), record.partition(), record.offset());
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
