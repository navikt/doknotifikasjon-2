package no.nav.doknotifikasjon.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(code = NOT_FOUND)
public class NotifikasjonIkkeFunnetException extends AbstractDoknotifikasjonFunctionalException {
	public NotifikasjonIkkeFunnetException(String message) {
		super(message);
	}
}
