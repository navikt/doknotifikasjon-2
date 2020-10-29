package no.nav.doknotifikasjon;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

@Slf4j
@Component
public class KafkaEventKnot004Consumer {

	private final ObjectMapper objectMapper;
	private final Knot004Service knot004Service;
	private final DoknotifikasjonStatusMapper doknotifikasjonStatusMapper;

	@Inject
	public KafkaEventKnot004Consumer(ObjectMapper objectMapper, Knot004Service knot004Service,
									 DoknotifikasjonStatusMapper doknotifikasjonStatusMapper) {
		this.objectMapper = objectMapper;
		this.knot004Service = knot004Service;
		this.doknotifikasjonStatusMapper = doknotifikasjonStatusMapper;
	}

	@KafkaListener(
			topics = "privat-dok-notifikasjon-status",
			containerFactory = "kafkaListenerContainerFactory",
			groupId = "doknotifikasjon-knot004"
	)
	@Metrics(value = "dok_request", percentiles = {0.5, 0.95})
	@Transactional
	public void onMessage(final ConsumerRecord<String, Object> record) {
		try {
			DoknotifikasjonStatus doknotifikasjonStatus = objectMapper.readValue(record.value()
					.toString(), DoknotifikasjonStatus.class);

			knot004Service.shouldUpdateStatus(doknotifikasjonStatusMapper.map(doknotifikasjonStatus));
		} catch (JsonProcessingException e) {
			log.error("Problemer med parsing av kafka-hendelse til Json. ", e);
		} catch (DoknotifikasjonValidationException e) {
			log.error("Valideringsfeil i knot004. Avslutter behandlingen. ", e);
		} catch (IllegalArgumentException e) {
			log.error("Valideringsfeil i knot004: Ugyldig status i hendelse på kafka-topic, avslutter behandlingen. ", e);
		}
	}
}
