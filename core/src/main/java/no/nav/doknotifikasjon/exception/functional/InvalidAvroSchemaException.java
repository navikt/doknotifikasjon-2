package no.nav.doknotifikasjon.exception.functional;

public class InvalidAvroSchemaException extends AbstractDoknotifikasjonFunctionalException {

	public InvalidAvroSchemaException(String message) {
		super(message);
	}

	public InvalidAvroSchemaException(String message, Throwable cause) {
		super(message, cause);
	}
}
