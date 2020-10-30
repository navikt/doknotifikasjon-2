package no.nav.doknotifikasjon.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DuplicateNotifikasjonInDBException;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.exception.functional.KontaktInfoValidationFunctionalException;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class Knot001Consumer {

	private final ObjectMapper objectMapper;
	private final Knot001Service knot001Service;
	private final DoknotifikasjonMapper doknotifikasjonMapper;
	private final DoknotifikasjonValidator doknotifikasjonValidator;

	Knot001Consumer(ObjectMapper objectMapper, Knot001Service knot001Service, DoknotifikasjonMapper doknotifikasjonMapper,
					DoknotifikasjonValidator doknotifikasjonValidator) {
		this.objectMapper = objectMapper;
		this.knot001Service = knot001Service;
		this.doknotifikasjonMapper = doknotifikasjonMapper;
		this.doknotifikasjonValidator = doknotifikasjonValidator;
	}

	@KafkaListener(
			topics = "privat-dok-notifikasjon",
			containerFactory = "kafkaListenerContainerFactory",
			groupId = "doknotifikasjon-knot001"
	)
	@Metrics(value = "dok_request", percentiles = {0.5, 0.95})
	@Transactional
	public void onMessage(final ConsumerRecord<String, Object> record) {
		try {
			Doknotifikasjon doknotifikasjon = objectMapper.readValue(record.value().toString(), Doknotifikasjon.class);
			doknotifikasjonValidator.validate(doknotifikasjon);
			knot001Service.processDoknotifikasjon(doknotifikasjonMapper.map(doknotifikasjon));
		} catch (JsonProcessingException e) {
			log.error("Problemer med parsing av kafka-hendelse til Json. Feilmelding: {}", e.getMessage());
		} catch (InvalidAvroSchemaFieldException e) {
			log.error("Validering av avroskjema feilet. Feilmelding: {}", e.getMessage());
		} catch (DuplicateNotifikasjonInDBException e) {
			log.error("BestlingsId ligger allerede i database. Feilmelding: {}", e.getMessage());
		} catch (KontaktInfoValidationFunctionalException e) {
			log.error("Brukeren har ikke gyldig kontaktinfo hos DKIF. Feilmelding: {}", e.getMessage());
		}
	}
}
