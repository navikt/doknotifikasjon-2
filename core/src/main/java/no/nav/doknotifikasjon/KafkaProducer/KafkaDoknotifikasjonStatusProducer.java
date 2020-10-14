package no.nav.doknotifikasjon.KafkaProducer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaDoknotifikasjonStatusProducer {

    @Autowired
    KafkaEventProducer producer;

    public void publishDoknotikfikasjonStatusOversendt(String topic, String bestillingsId, String bestillerId,
                                                       String melding, Long distribusjonId)
    {
        this.publishDoknotifikasjonStatus(topic, bestillingsId, bestillerId, Status.OVERSENDT, melding, distribusjonId);
    }

    public void publishDoknotikfikasjonStatusOpprettet(String topic, String bestillingsId, String bestillerId,
                                                       String melding, Long distribusjonId)
    {
        this.publishDoknotifikasjonStatus(topic, bestillingsId, bestillerId, Status.OPPRETTET, melding, distribusjonId);
    }

    public void publishDoknotikfikasjonStatusFerdigstilt(String topic, String bestillingsId, String bestillerId,
                                                       String melding, Long distribusjonId)
    {
        this.publishDoknotifikasjonStatus(topic, bestillingsId, bestillerId, Status.FERDIGSTILT, melding, distribusjonId);
    }

    public void publishDoknotikfikasjonStatusFeilet(String topic, String bestillingsId, String bestillerId,
                                                    String melding, Long distribusjonId)
    {
        this.publishDoknotifikasjonStatus(
                topic,
                bestillingsId == null ? "Ukjent" : bestillingsId,
                bestillerId == null ? "Ukjent" : bestillingsId,
                Status.FEILET,
                melding,
                distribusjonId
        );
    }

    private void publishDoknotifikasjonStatus(String topic, String bestillingsId, String bestillerId,
                                              Status status, String melding, Long distribusjonId)
    {
        DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(bestillingsId, bestillerId, status.toString(), melding, distribusjonId);
        this.publishDoknotifikasjonStatus(topic, doknotifikasjonStatus);
    }

    public void publishDoknotifikasjonStatus(String topic, DoknotifikasjonStatus doknotifikasjonStatus)
    {
        producer.publish(
                topic,
                doknotifikasjonStatus,
                System.currentTimeMillis()
        );
    }
}
