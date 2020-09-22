package no.nav.doknotifikasjon.exception.functional;

public abstract class AbstractDoknotifikasjonFunctionalException extends RuntimeException {

	public AbstractDoknotifikasjonFunctionalException(String message) {
		super(message);
	}

	public AbstractDoknotifikasjonFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}

