package no.nav.doknotifikasjon.exception.functional;

public class UgyldigEpostException extends AbstractDoknotifikasjonFunctionalException {

	public UgyldigEpostException(String message) {
		super(message);
	}

	public UgyldigEpostException(String message, Throwable cause) {
		super(message, cause);
	}
}
