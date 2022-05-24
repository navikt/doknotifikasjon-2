package no.nav.doknotifikasjon.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.springframework.stereotype.Component;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS;

@Slf4j
@Component
public class KafkaStatusEventProducer {

	private final KafkaEventProducer producer;

	KafkaStatusEventProducer(KafkaEventProducer producer) {
		this.producer = producer;
	}

	public void publishDoknotifikasjonStatusOversendt(String bestillingsId, String bestillerId,
													  String melding, Long distribusjonId) {
		this.publishDoknotifikasjonStatus(bestillingsId, bestillerId, Status.OVERSENDT, melding, distribusjonId);
	}

	public void publishDoknotifikasjonStatusFerdigstilt(String bestillingsId, String bestillerId,
														String melding, Long distribusjonId) {
		this.publishDoknotifikasjonStatus(bestillingsId, bestillerId, Status.FERDIGSTILT, melding, distribusjonId);
	}

	public void publishDoknotifikasjonStatusInfo(String bestillingsId, String bestillerId,
												 String melding, Long distribusjonId) {
		this.publishDoknotifikasjonStatus(bestillingsId, bestillerId, Status.INFO, melding, distribusjonId);
	}

	public void publishDoknotifikasjonStatusFeilet(String bestillingsId, String bestillerId,
												   String melding, Long distribusjonId) {
		this.publishDoknotifikasjonStatus(
				bestillingsId == null ? "Ukjent" : bestillingsId,
				bestillerId == null ? "Ukjent" : bestillerId,
				Status.FEILET,
				melding,
				distribusjonId
		);
	}

	public void publishDoknotifikasjonStatus(String bestillingsId, String bestillerId,
											 Status status, String melding, Long distribusjonId) {
		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(bestillingsId, bestillerId, status.toString(), melding, distribusjonId);
		this.publishDoknotifikasjonStatus(doknotifikasjonStatus);
	}

	public void publishDoknotifikasjonStatus(DoknotifikasjonStatus doknotifikasjonStatus) {
		producer.publish(
				KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS,
				doknotifikasjonStatus
		);
	}
}
