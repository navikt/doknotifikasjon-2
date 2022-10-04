package no.nav.doknotifikasjon.exception.functional;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class NotifikasjonIkkeFunnetException extends AbstractDoknotifikasjonFunctionalException {
	public NotifikasjonIkkeFunnetException(String message) {
		super(message);
	}
}
