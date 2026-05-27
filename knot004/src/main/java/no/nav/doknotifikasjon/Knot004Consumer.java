package no.nav.doknotifikasjon;


import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearCallId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.generateNewCallIdIfThereAreNone;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT004_CONSUMER;

@Slf4j
@Component
public class Knot004Consumer {

	private final JsonMapper jsonMapper;
	private final MetricService metricService;
	private final Knot004Service knot004Service;
	private final DoknotifikasjonStatusMapper doknotifikasjonStatusMapper;

	public Knot004Consumer(JsonMapper jsonMapper, Knot004Service knot004Service,
						   DoknotifikasjonStatusMapper doknotifikasjonStatusMapper,
						   MetricService metricService) {
		this.jsonMapper = jsonMapper;
		this.knot004Service = knot004Service;
		this.doknotifikasjonStatusMapper = doknotifikasjonStatusMapper;
		this.metricService = metricService;
	}

	@KafkaListener(
			topics = KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS,
			groupId = "doknotifikasjon-knot004",
			autoStartup = "${autostartup.av.kafkalyttere.for.knot}"
	)
	@Metrics(value = DOK_KNOT004_CONSUMER, createErrorMetric = true)
	@Transactional
	public void onMessage(final ConsumerRecord<String, Object> record) {
		generateNewCallIdIfThereAreNone(record.key());
		log.info("Knot004 Innkommende kafka record til topic={}, partition={}, offset={}", record.topic(), record.partition(), record.offset());

		try {
			DoknotifikasjonStatus doknotifikasjonStatus = jsonMapper.readValue(record.value().toString(), DoknotifikasjonStatus.class);
			knot004Service.shouldUpdateStatus(doknotifikasjonStatusMapper.map(doknotifikasjonStatus));
		} catch (JacksonException e) {
			log.error("Knot004 problemer med parsing av kafka-hendelse til Json. ", e);
			metricService.metricHandleException(e);
		} catch (DoknotifikasjonValidationException e) {
			log.error("Knot004 valideringsfeil. Avslutter behandlingen. ", e);
			metricService.metricHandleException(e);
		} catch (IllegalArgumentException e) {
			log.warn("Knot004 valideringsfeil: Ugyldig status i hendelse på kafka-topic, avslutter behandlingen. ", e);
			metricService.metricHandleException(e);
		} catch (Exception e) {
			log.error("Knot004 ukjent teknisk feil. Konsumerer hendelse på nytt. Dette må følges opp.", e);
			metricService.metricHandleException(e);
			throw e;
		} finally {
			clearCallId();
		}
	}
}
