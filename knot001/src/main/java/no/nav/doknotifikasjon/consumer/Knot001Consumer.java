package no.nav.doknotifikasjon.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DigitalKontaktinformasjonFunctionalException;
import no.nav.doknotifikasjon.exception.functional.DuplicateNotifikasjonInDBException;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.exception.functional.KontaktInfoValidationFunctionalException;
import no.nav.doknotifikasjon.exception.functional.SikkerhetsnivaaFunctionalException;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearCallId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.generateNewCallIdIfThereAreNone;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT001_CONSUMER;

@Slf4j
@Component
public class Knot001Consumer {

	private final ObjectMapper objectMapper;
	private final MetricService metricService;
	private final Knot001Service knot001Service;
	private final DoknotifikasjonMapper doknotifikasjonMapper;
	private final DoknotifikasjonValidator doknotifikasjonValidator;

	@Autowired
	Knot001Consumer(
			ObjectMapper objectMapper,
			Knot001Service knot001Service,
			DoknotifikasjonMapper doknotifikasjonMapper,
			MetricService metricService,
			DoknotifikasjonValidator doknotifikasjonValidator
	) {
		this.objectMapper = objectMapper;
		this.knot001Service = knot001Service;
		this.doknotifikasjonMapper = doknotifikasjonMapper;
		this.doknotifikasjonValidator = doknotifikasjonValidator;
		this.metricService = metricService;
	}

	@KafkaListener(
			topics = KAFKA_TOPIC_DOK_NOTIFIKASJON,
			groupId = "doknotifikasjon-knot001"
	)
	@Transactional
	@Metrics(value = DOK_KNOT001_CONSUMER, createErrorMetric = true)
	public void onMessage(final ConsumerRecord<String, Object> record) {
		generateNewCallIdIfThereAreNone(record.key());
		log.info("Knot001 Innkommende kafka record til topic={}, partition={}, offset={}", record.topic(), record.partition(), record.offset());

		try {
			Doknotifikasjon doknotifikasjon = objectMapper.readValue(record.value().toString(), Doknotifikasjon.class);
			doknotifikasjonValidator.validate(doknotifikasjon);
			knot001Service.processDoknotifikasjon(doknotifikasjonMapper.map(doknotifikasjon));
			metricService.metricKnot001RecordBehandlet();
		} catch (JsonProcessingException e) {
			log.error("Problemer med parsing av kafka-hendelse til Json. Feilmelding={}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (InvalidAvroSchemaFieldException e) {
			log.error("Validering av Doknotifikasjon-melding feilet. Feilmelding={}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (DuplicateNotifikasjonInDBException e) {
			log.warn("BestillingsId ligger allerede i database. Feilmelding={}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (KontaktInfoValidationFunctionalException e) {
			log.warn("Brukeren har ikke gyldig kontaktinfo hos Digdir KRR Proxy. Feilmelding={}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (DigitalKontaktinformasjonFunctionalException e) {
			log.warn("Funksjonell feil mot Digdir KRR Proxy. Feilmelding={}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (SikkerhetsnivaaFunctionalException e) {
			log.warn("Sjekk mot sikkerhetsnivaa feilet: Mottaker har ikke tilgang til login på nivå 4. Feilmelding={}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (DataIntegrityViolationException e) {
			log.error("Får ikke persistert bestilling til database. Feilmelding={}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (Exception e) {
			log.error("Ukjent teknisk feil for knot001. Konsumerer hendelse på nytt. Dette må følges opp.", e);
			metricService.metricHandleException(e);
			throw e;
		} finally {
			clearCallId();
		}
	}
}
