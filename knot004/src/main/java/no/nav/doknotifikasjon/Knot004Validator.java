package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.exception.functional.Knot004ValidationException;
import no.nav.doknotifikasjon.model.Notifikasjon;
import org.springframework.stereotype.Component;

@Component
public class Knot004Validator {

	public void shouldValidateInput(DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
		isNullOrEmpty(doknotifikasjonStatusTo.getBestillingId(), "bestillingId");
		isNullOrEmpty(doknotifikasjonStatusTo.getBestillerId(), "bestillerId");
		isNullOrEmpty(doknotifikasjonStatusTo.getStatus(), "status");
		isNullOrEmpty(doknotifikasjonStatusTo.getMelding(), "melding");
	}

	private void isNullOrEmpty(String field, String fieldName) throws Knot004ValidationException {
		if (field == null || field.isEmpty()) {
			throw new Knot004ValidationException(String.format("Feltet %s er null eller tomt. ", fieldName));
		}
	}
}
