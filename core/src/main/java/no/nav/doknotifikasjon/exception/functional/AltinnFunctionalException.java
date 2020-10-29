package no.nav.doknotifikasjon.exception.functional;

import javax.xml.namespace.QName;

public class AltinnFunctionalException extends AbstractDoknotifikasjonFunctionalException {

	public AltinnFunctionalException(String message) {
		super(message);
	}

	public AltinnFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
