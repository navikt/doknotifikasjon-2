package no.nav.doknotifikasjon.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearCallId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearDistribusjonId;
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

	@Inject
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

	@SneakyThrows
	@KafkaListener(
			topics = KAFKA_TOPIC_DOK_NOTIFKASJON,
			containerFactory = "kafkaListenerContainerFactory",
			groupId = "doknotifikasjon-knot001"
	)
	@Metrics(value = DOK_KNOT001_CONSUMER, createErrorMetric = true)
	public void onMessage(final ConsumerRecord<String, Object> record) {
		generateNewCallIdIfThereAreNone(record.key());
		log.info("Innkommende kafka record til topic: {}, partition: {}, offset: {}", record.topic(), record.partition(), record.offset());

		try {
			Doknotifikasjon doknotifikasjon = objectMapper.readValue(record.value().toString(), Doknotifikasjon.class);
			doknotifikasjonValidator.validate(doknotifikasjon);
			knot001Service.processDoknotifikasjon(doknotifikasjonMapper.map(doknotifikasjon));
			metricService.metricKnot001RecordBehandlet();
		} catch (JsonProcessingException e) {
			log.error("Problemer med parsing av kafka-hendelse til Json. Feilmelding: {}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (InvalidAvroSchemaFieldException e) {
			log.error("Validering av avroskjema feilet. Feilmelding: {}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (DuplicateNotifikasjonInDBException e) {
			log.error("BestlingsId ligger allerede i database. Feilmelding: {}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (KontaktInfoValidationFunctionalException e) {
			log.error("Brukeren har ikke gyldig kontaktinfo hos DKIF. Feilmelding: {}", e.getMessage());
			metricService.metricHandleException(e);
		} catch(DigitalKontaktinformasjonFunctionalException e){
			log.error("Funksjonell feil mot DKIF. Feilmelding: {}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (SikkerhetsnivaaFunctionalException e) {
			log.warn("Sjekk mot sikkerhetsnivaa feilet: Mottaker har ikke tilgang til login på nivå 4. Feilmelding={}", e.getMessage());
			metricService.metricHandleException(e);
		} finally {
			clearCallId();
		}
	}
}
