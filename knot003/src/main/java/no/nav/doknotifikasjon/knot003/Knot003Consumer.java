package no.nav.doknotifikasjon.knot003;


import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_EPOST;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearCallId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearDistribusjonId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.generateNewCallIdIfThereAreNone;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.setDistribusjonId;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT003_CONSUMER;


@Slf4j
@Component
public class Knot003Consumer {

	private final JsonMapper jsonMapper;
	private final Knot003Service knot003Service;
	private final MetricService metricService;

	Knot003Consumer(Knot003Service knot003Service, JsonMapper jsonMapper, MetricService metricService) {
		this.knot003Service = knot003Service;
		this.jsonMapper = jsonMapper;
		this.metricService = metricService;
	}

	@KafkaListener(
			topics = KAFKA_TOPIC_DOK_NOTIFIKASJON_EPOST,
			groupId = "doknotifikasjon-knot003",
			autoStartup = "${autostartup.av.kafkalyttere.for.knot}"
	)
	@Transactional
	@Metrics(value = DOK_KNOT003_CONSUMER, createErrorMetric = true)
	public void onMessage(final ConsumerRecord<String, Object> record) {
		generateNewCallIdIfThereAreNone(record.key());
		log.info("Knot003 Innkommende kafka record til topic={}, partition={}, offset={}", record.topic(), record.partition(), record.offset());

		try {
			DoknotifikasjonEpost doknotifikasjonEpost = jsonMapper.readValue(record.value().toString(), DoknotifikasjonEpost.class);
			setDistribusjonId(String.valueOf(doknotifikasjonEpost.getNotifikasjonDistribusjonId()));

			log.info("Knot003 starter behandling av notifikasjonDistribusjon med id={}", doknotifikasjonEpost.getNotifikasjonDistribusjonId());
			knot003Service.sendEpost(doknotifikasjonEpost.getNotifikasjonDistribusjonId());
			metricService.metricKnot003EpostSent();
		} catch (JacksonException e) {
			log.error("Knot003 har problemer med parsing av kafka-hendelse til Json. ", e);
			metricService.metricHandleException(e);
		} catch (IllegalArgumentException e) {
			log.warn("Knot003 valideringsfeil: Ugyldig status i hendelse på kafka-topic, avslutter behandlingen. ", e);
			metricService.metricHandleException(e);
		} catch (DoknotifikasjonDistribusjonIkkeFunnetException e) {
			log.error("Knot003 finner ikke notifikasjonDistribusjonId={} etter flere forsøk. Gir opp denne hendelsen", e.getNotifikasjonDistribusjonId());
			metricService.metricHandleException(e);
		} catch (Exception e) {
			log.error("Knot003 ukjent teknisk feil. Konsumerer hendelse på nytt. Dette må følges opp.", e);
			metricService.metricHandleException(e);
			throw e;
		} finally {
			clearDistribusjonId();
			clearCallId();
		}
	}
}
