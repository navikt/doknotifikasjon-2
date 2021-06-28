package no.nav.doknotifikasjon.knot003;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.metrics.MetricService;
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
	private final MetricService metricService;

	@Inject
	Knot003Consumer(Knot003Service knot003Service, ObjectMapper objectMapper, MetricService metricService) {
		this.knot003Service = knot003Service;
		this.objectMapper = objectMapper;
		this.metricService = metricService;
	}

	@KafkaListener(
			topics = KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST,
			containerFactory = "kafkaListenerContainerFactory",
			groupId = "doknotifikasjon-knot003"
	)
	@Transactional
	@Metrics(value = DOK_KNOT003_CONSUMER, createErrorMetric = true)
	public void onMessage(final ConsumerRecord<String, Object> record) {
		generateNewCallIdIfThereAreNone(record.key());
		log.info("Innkommende kafka record til topic={}, partition={}, offset={}", record.topic(), record.partition(), record.offset());

		try {
			DoknotifikasjonEpost doknotifikasjonEpost = objectMapper.readValue(record.value().toString(), DoknotifikasjonEpost.class);
			setDistribusjonId(String.valueOf(doknotifikasjonEpost.getNotifikasjonDistribusjonId()));

			log.info("Knot003 starter behandling av notifikasjonDistribusjon med id={}", doknotifikasjonEpost.getNotifikasjonDistribusjonId());
			knot003Service.shouldSendEpost(doknotifikasjonEpost.getNotifikasjonDistribusjonId());
		} catch (JsonProcessingException e) {
			log.error("Problemer med parsing av kafka-hendelse til Json. ", e);
			metricService.metricHandleException(e);
		} catch (DoknotifikasjonDistribusjonIkkeFunnetException e) {
			log.warn("Ingen notifikasjonDistribusjon ble funnet i databasen. Avslutter behandlingen. ", e);
			metricService.metricHandleException(e);
		} catch (DoknotifikasjonValidationException e) {
			log.error("Valideringsfeil i knot003. Avslutter behandlingen. ", e);
			metricService.metricHandleException(e);
		} catch (AltinnFunctionalException e){
			log.warn("Knot002 NotifikasjonDistribusjonConsumer funksjonell feil ved kall mot Altinn. ", e);
			metricService.metricHandleException(e);
		} catch (IllegalArgumentException e) {
			log.warn("Valideringsfeil i knot003: Ugyldig status i hendelse på kafka-topic, avslutter behandlingen. ", e);
			metricService.metricHandleException(e);
		} finally {
			clearDistribusjonId();
			clearCallId();
		}
	}
}
