package no.nav.doknotifikasjon.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DuplicateNotifikasjonInDBException;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.PRIVAT_DOK_NOTIFIKASJON_MED_KONTAKT_INFO;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.clearCallId;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.generateNewCallIdIfThereAreNone;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT006_CONSUMER;

@Slf4j
@Component
public class Knot006Consumer {

	private final ObjectMapper objectMapper;
	private final MetricService metricService;
	private final Knot006Service knot006Service;
	private final DoknotifikasjonMedKontaktInfoMapper doknotifikasjonMedKontaktInfoMapper;
	private final NotifikasjonValidator doknotifikasjonValidator;

	Knot006Consumer(
			ObjectMapper objectMapper,
			Knot006Service knot006Service,
			DoknotifikasjonMedKontaktInfoMapper doknotifikasjonMedKontaktInfoMapper,
			MetricService metricService,
			NotifikasjonValidator doknotifikasjonValidator
	) {
		this.objectMapper = objectMapper;
		this.knot006Service = knot006Service;
		this.doknotifikasjonMedKontaktInfoMapper = doknotifikasjonMedKontaktInfoMapper;
		this.doknotifikasjonValidator = doknotifikasjonValidator;
		this.metricService = metricService;
	}

	@KafkaListener(
			topics = PRIVAT_DOK_NOTIFIKASJON_MED_KONTAKT_INFO,
			groupId = "doknotifikasjon-knot006",
			autoStartup = "${autostartup.av.kafkalyttere.for.knot}"
	)
	@Transactional
	@Metrics(value = DOK_KNOT006_CONSUMER, createErrorMetric = true)
	public void onMessage(final ConsumerRecord<String, Object> record) {
		generateNewCallIdIfThereAreNone(record.key());
		log.info("Knot006 Innkommende kafka record til topic={}, partition={}, offset={}", record.topic(), record.partition(), record.offset());

		try {
			NotifikasjonMedkontaktInfo notifikasjonMedkontaktInfo = objectMapper.readValue(record.value().toString(), NotifikasjonMedkontaktInfo.class);
			doknotifikasjonValidator.validate(notifikasjonMedkontaktInfo);
			knot006Service.processNotifikasjonMedkontaktInfo(doknotifikasjonMedKontaktInfoMapper.map(notifikasjonMedkontaktInfo));
			metricService.metricKnot006RecordBehandlet();
		} catch (JsonProcessingException e) {
			log.warn("Problemer med parsing av kafka-hendelse til Json. Feilmelding: {}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (InvalidAvroSchemaFieldException e) {
			log.error("Validering av NotifikasjonMedkontaktInfo-melding feilet. Feilmelding: {}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (DuplicateNotifikasjonInDBException e) {
			log.warn("BestlingsId ligger allerede i database. Feilmelding: {}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (DataIntegrityViolationException e) {
			log.error("Får ikke persistert bestilling til database. Feilmelding={}", e.getMessage());
			metricService.metricHandleException(e);
		} catch (Exception e) {
			log.error("Ukjent teknisk feil for knot006. Konsumerer hendelse på nytt. Dette må følges opp.", e);
			metricService.metricHandleException(e);
			throw e;
		} finally {
			clearCallId();
		}
	}
}
