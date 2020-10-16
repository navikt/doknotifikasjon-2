package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.kodeverk.Status;
import org.springframework.stereotype.Component;

@Component
public class DoknotifikasjonStatusValidator {

    public void shouldValidateInput(DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
        isNullOrEmpty(doknotifikasjonStatusTo.getBestillingId(), "bestillingId");
        isNullOrEmpty(doknotifikasjonStatusTo.getBestillerId(), "bestillerId");
        isNullOrEmpty(doknotifikasjonStatusTo.getMelding(), "melding");
        validateStatus(doknotifikasjonStatusTo.getStatus());
    }

    private void isNullOrEmpty(String field, String fieldName) {
        if (field == null || field.isEmpty()) {
            throw new DoknotifikasjonValidationException(String.format("Valideringsfeil i knot004: Hendelse på kafka-topic " +
                    "dok-eksternnotifikasjon-status har tom variabel: %s. ", fieldName));
        }
    }

    private void validateStatus(String status) {
        isNullOrEmpty(status, "status");
        try {
            Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new DoknotifikasjonValidationException(String.format("Valideringsfeil i knot004: Hendelse på kafka-topic " +
                    "dok-eksternnotifikasjon-status har ugyldig status: %s.", status));
        }
    }
}
