package no.nav.doknotifikasjon.exception.functional;

import org.springframework.http.HttpStatus;

public class DigitalKontaktinformasjonFunctionalException extends AbstractDoknotifikasjonFunctionalException {

	private final HttpStatus httpStatus;

	public DigitalKontaktinformasjonFunctionalException(String message, Throwable cause, HttpStatus status) {
		super(message, cause);
		httpStatus = status;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
}
