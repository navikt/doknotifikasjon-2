package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.kafka.KafkaTopics;
import org.springframework.stereotype.Component;

@Component
public class DoknotifikasjonStoppValidadator {

    public void validateInput(DoknotifikasjonStoppTo doknotifikasjonStoppTo) {
        validateField(doknotifikasjonStoppTo.getBestillerId(), "bestillerId");
        validateField(doknotifikasjonStoppTo.getBestillingsId(), "bestillingsId");
    }

    private void validateField(String field, String fieldName) {
        if (field == null || field.trim().isEmpty()) {
            throw new DoknotifikasjonValidationException(String.format("Valideringsfeil i knot005: Hendelse på kafka-topic " +
                    "%s har tom verdi for %s.", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STOPP, fieldName));
        }
    }
}
