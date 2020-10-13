package no.nav.doknotifikasjon.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.DoknotifikasjonStatusDto;
import no.nav.doknotifikasjon.DoknotifikasjonStatusMapper;
import no.nav.doknotifikasjon.Knot004Service;
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
	private final DoknotifikasjonStatusMapper mapper;
	private final Knot004Service service;

	@Inject
	public KafkaEventKnot004Consumer(ObjectMapper objectMapper, DoknotifikasjonStatusMapper mapper, Knot004Service service) {
		this.objectMapper = objectMapper;
		this.mapper = mapper;
		this.service = service;
	}

	@KafkaListener(
			topics = "privat-dok-ekstern-notifikasjon-status",
			containerFactory = "kafkaListenerContainerFactory"
	)
//	@Metrics(value = "dok_request", percentiles = {0.5, 0.95})  // Lage metrics? Grafana?
	@Transactional
	public void onMessage(final ConsumerRecord<String, Object> record) {
		DoknotifikasjonStatus doknotifikasjonStatus = null;

		try {
			doknotifikasjonStatus = objectMapper.readValue(record.value().toString(), DoknotifikasjonStatus.class);

			service.shouldUpdateStatus(mapper.map(doknotifikasjonStatus));
		} catch (JsonProcessingException e) {
//            e.printStackTrace();
		} catch(Exception e){}


		doknotifikasjonStatus.getBestillerId();
	}
}
