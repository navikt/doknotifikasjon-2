package no.nav.doknotifikasjon.exception.functional;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class NotifikasjonIkkeFunnetException extends AbstractDoknotifikasjonFunctionalException {
	public NotifikasjonIkkeFunnetException(String message) {
		super(message);
	}
}
