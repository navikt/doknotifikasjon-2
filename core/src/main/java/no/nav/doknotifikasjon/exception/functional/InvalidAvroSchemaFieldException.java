package no.nav.doknotifikasjon.exception.functional;

public class InvalidAvroSchemaFieldException extends AbstractDoknotifikasjonFunctionalException {

	public InvalidAvroSchemaFieldException(String message) {
		super(message);
	}

	public InvalidAvroSchemaFieldException(String message, Throwable cause) {
		super(message, cause);
	}
}
