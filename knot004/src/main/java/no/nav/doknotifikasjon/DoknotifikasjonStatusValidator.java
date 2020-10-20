package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.utils.KafkaTopics;
import org.springframework.stereotype.Component;

@Component
public class DoknotifikasjonStatusValidator {

    public void validateInput(DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
        isNullOrEmpty(doknotifikasjonStatusTo.getBestillingsId(), "bestillingsId");
        isNullOrEmpty(doknotifikasjonStatusTo.getBestillerId(), "bestillerId");
        isNullOrEmpty(doknotifikasjonStatusTo.getMelding(), "melding");
        validateStatus(doknotifikasjonStatusTo.getStatus());
    }

    private void isNullOrEmpty(String field, String fieldName) {
        if (field == null || field.trim().isEmpty()) {
            throw new DoknotifikasjonValidationException(String.format("Valideringsfeil i knot004: Hendelse på kafka-topic " +
                    "%s har tom verdi for %s.", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS, fieldName));
        }
    }

    private void validateStatus(Status status) {
        if (status == null) {
            throw new DoknotifikasjonValidationException(String.format("Valideringsfeil i knot004: Hendelse på kafka-topic " +
                    "%s har tom verdi for status.", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS));
        }
    }
}
