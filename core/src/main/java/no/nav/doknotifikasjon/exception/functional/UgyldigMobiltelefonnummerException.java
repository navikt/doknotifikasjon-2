package no.nav.doknotifikasjon.exception.functional;

public class UgyldigMobiltelefonnummerException extends AbstractDoknotifikasjonFunctionalException {

	public UgyldigMobiltelefonnummerException(String message) {
		super(message);
	}

	public UgyldigMobiltelefonnummerException(String message, Throwable cause) {
		super(message, cause);
	}
}
