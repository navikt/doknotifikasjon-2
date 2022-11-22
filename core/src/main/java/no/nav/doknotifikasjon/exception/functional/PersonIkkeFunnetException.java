package no.nav.doknotifikasjon.exception.functional;

import no.nav.doknotifikasjon.exception.functional.AbstractDoknotifikasjonFunctionalException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
public class PersonIkkeFunnetException extends AbstractDoknotifikasjonFunctionalException {

	public PersonIkkeFunnetException(String message) {
		super(message);
	}
}
