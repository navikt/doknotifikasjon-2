package no.nav.doknotifikasjon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STOPP;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearCallId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.generateNewCallIdIfThereAreNone;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT005_CONSUMER;

@Slf4j
@Component
public class Knot005Consumer {

	private final ObjectMapper objectMapper;
	private final Knot005Service knot005Service;
	private final MetricService metricService;
	private final DoknotifikasjonStoppMapper doknotifikasjonStoppMapper;

	public Knot005Consumer(ObjectMapper objectMapper, Knot005Service knot005Service,
						   DoknotifikasjonStoppMapper doknotifikasjonStoppMapper,
						   MetricService metricService) {
		this.objectMapper = objectMapper;
		this.knot005Service = knot005Service;
		this.doknotifikasjonStoppMapper = doknotifikasjonStoppMapper;
		this.metricService = metricService;
	}

	@KafkaListener(
			topics = KAFKA_TOPIC_DOK_NOTIFIKASJON_STOPP,
			groupId = "doknotifikasjon-knot005",
			autoStartup = "${autostartup.av.kafkalyttere.for.knot}"
	)
	@Metrics(value = DOK_KNOT005_CONSUMER, createErrorMetric = true)
	@Transactional
	public void onMessage(final ConsumerRecord<String, Object> record) {
		generateNewCallIdIfThereAreNone(record.key());
		log.info("Knot005 Innkommende kafka record til topic={}, partition={}, offset={}", record.topic(), record.partition(), record.offset());

		try {
			DoknotifikasjonStopp doknotifikasjonStopp = objectMapper.readValue(record.value().toString(),
					DoknotifikasjonStopp.class);
			knot005Service.shouldStopResending(doknotifikasjonStoppMapper.map(doknotifikasjonStopp));
		} catch (JsonProcessingException e) {
			log.error("Problemer med parsing av kafka-hendelse til Json. Feilmelding: {}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (DoknotifikasjonValidationException e) {
			log.warn("Valideringsfeil oppstod i knot005. Feilmelding: {}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (Exception e) {
			log.error("Ukjent teknisk feil for knot005 (stopp). Konsumerer hendelse på nytt. Dette må følges opp.", e);
			metricService.metricHandleException(e);
			throw e;
		} finally {
			clearCallId();
		}
	}
}
