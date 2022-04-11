package no.nav.doknotifikasjon;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearCallId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.generateNewCallIdIfThereAreNone;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT004_CONSUMER;

@Slf4j
@Component
public class Knot004Consumer {

	private final ObjectMapper objectMapper;
	private final MetricService metricService;
	private final Knot004Service knot004Service;
	private final DoknotifikasjonStatusMapper doknotifikasjonStatusMapper;

	@Autowired
	public Knot004Consumer(ObjectMapper objectMapper, Knot004Service knot004Service,
						   DoknotifikasjonStatusMapper doknotifikasjonStatusMapper,
						   MetricService metricService) {
		this.objectMapper = objectMapper;
		this.knot004Service = knot004Service;
		this.doknotifikasjonStatusMapper = doknotifikasjonStatusMapper;
		this.metricService = metricService;
	}

	@KafkaListener(
			topics = KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
			containerFactory = "kafkaListenerContainerFactory",
			groupId = "doknotifikasjon-knot004"
	)
	@Metrics(value = DOK_KNOT004_CONSUMER, createErrorMetric = true)
	@Transactional
	public void onMessage(final ConsumerRecord<String, Object> record) {
		generateNewCallIdIfThereAreNone(record.key());
		log.info("Innkommende kafka record til topic={}, partition={}, offset={}", record.topic(), record.partition(), record.offset());

		try {
			DoknotifikasjonStatus doknotifikasjonStatus = objectMapper.readValue(record.value().toString(), DoknotifikasjonStatus.class);
			knot004Service.shouldUpdateStatus(doknotifikasjonStatusMapper.map(doknotifikasjonStatus));
		} catch (JsonProcessingException e) {
			log.error("Problemer med parsing av kafka-hendelse til Json. ", e);
			metricService.metricHandleException(e);
		} catch (DoknotifikasjonValidationException e) {
			log.error("Valideringsfeil i knot004. Avslutter behandlingen. ", e);
			metricService.metricHandleException(e);
		} catch (IllegalArgumentException e) {
			log.warn("Valideringsfeil i knot004: Ugyldig status i hendelse på kafka-topic, avslutter behandlingen. ", e);
			metricService.metricHandleException(e);
		} catch (Exception e) {
			log.error("Ukjent teknisk feil for knot004 (status). Konsumerer hendelse på nytt. Dette må følges opp.", e);
			metricService.metricHandleException(e);
			throw e;
		} finally {
			clearCallId();
		}
	}
}