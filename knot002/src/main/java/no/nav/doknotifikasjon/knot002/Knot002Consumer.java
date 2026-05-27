package no.nav.doknotifikasjon.knot002;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_SMS;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearCallId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearDistribusjonId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.generateNewCallIdIfThereAreNone;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.setDistribusjonId;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT002_CONSUMER;

@Slf4j
@Component
public class Knot002Consumer {

	private final JsonMapper jsonMapper;
	private final Knot002Service knot002Service;
	private final MetricService metricService;

	Knot002Consumer(Knot002Service knot002Service, JsonMapper jsonMapper, MetricService metricService) {
		this.knot002Service = knot002Service;
		this.jsonMapper = jsonMapper;
		this.metricService = metricService;
	}

	@KafkaListener(
			topics = KAFKA_TOPIC_DOK_NOTIFIKASJON_SMS,
			groupId = "doknotifikasjon-knot002",
			autoStartup = "${autostartup.av.kafkalyttere.for.knot}"
	)
	@Transactional
	@Metrics(value = DOK_KNOT002_CONSUMER, createErrorMetric = true)
	public void onMessage(final ConsumerRecord<String, Object> record) {
		generateNewCallIdIfThereAreNone(record.key());
		log.info("Knot002 Innkommende kafka record til topic={}, partition={}, offset={}", record.topic(), record.partition(), record.offset());

		try {
			DoknotifikasjonSms doknotifikasjonSms = jsonMapper.readValue(record.value().toString(), DoknotifikasjonSms.class);
			setDistribusjonId(String.valueOf(doknotifikasjonSms.getNotifikasjonDistribusjonId()));

			log.info("Knot002 starter behandling av notifikasjonDistribusjon med id={}", doknotifikasjonSms.getNotifikasjonDistribusjonId());
			knot002Service.sendSms(doknotifikasjonSms.getNotifikasjonDistribusjonId());
			metricService.metricKnot002SmsSent();
		} catch (JacksonException e) {
			log.error("Knot002 har problemer med parsing av kafka-hendelse til Json. ", e);
			metricService.metricHandleException(e);
		} catch (DoknotifikasjonDistribusjonIkkeFunnetException e) {
			log.error("Knot002 finner ikke notifikasjonDistribusjonId={} etter flere forsøk. Gir opp denne hendelsen", e.getNotifikasjonDistribusjonId());
			metricService.metricHandleException(e);
		} catch (Exception e) {
			log.error("Knot002 ukjent teknisk feil. Konsumerer hendelse på nytt. Dette må følges opp.", e);
			metricService.metricHandleException(e);
			throw e;
		} finally {
			clearDistribusjonId();
			clearCallId();
		}
	}
}
