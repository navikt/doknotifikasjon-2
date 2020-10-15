package no.nav.doknotifikasjon.KafkaProducer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS;

@Slf4j
@Component
public class KafkaDoknotifikasjonStatusProducer {

    @Autowired
    KafkaEventProducer producer;

    public void publishDoknotikfikasjonStatusOversendt(String bestillingsId, String bestillerId,
                                                       String melding, Long distribusjonId)
    {
        this.publishDoknotifikasjonStatus(bestillingsId, bestillerId, Status.OVERSENDT, melding, distribusjonId);
    }

    public void publishDoknotikfikasjonStatusOpprettet(String bestillingsId, String bestillerId,
                                                       String melding, Long distribusjonId)
    {
        this.publishDoknotifikasjonStatus(bestillingsId, bestillerId, Status.OPPRETTET, melding, distribusjonId);
    }

    public void publishDoknotikfikasjonStatusFerdigstilt(String bestillingsId, String bestillerId,
                                                       String melding, Long distribusjonId)
    {
        this.publishDoknotifikasjonStatus(bestillingsId, bestillerId, Status.FERDIGSTILT, melding, distribusjonId);
    }

    public void publishDoknotikfikasjonStatusFeilet(String bestillingsId, String bestillerId,
                                                    String melding, Long distribusjonId)
    {
        this.publishDoknotifikasjonStatus(
                bestillingsId == null ? "Ukjent" : bestillingsId,
                bestillerId == null ? "Ukjent" : bestillingsId,
                Status.FEILET,
                melding,
                distribusjonId
        );
    }

    public void publishDoknotifikasjonStatus(String bestillingsId, String bestillerId,
                                              Status status, String melding, Long distribusjonId)
    {
        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(bestillingsId, bestillerId, status.toString(), melding, distribusjonId);
        this.publishDoknotifikasjonStatus(doknotifikasjonStatus);
    }

    public void publishDoknotifikasjonStatus(DoknotifikasjonStatus doknotifikasjonStatus)
    {
        producer.publish(
                KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS,
                doknotifikasjonStatus,
                System.currentTimeMillis()
        );
    }
}
