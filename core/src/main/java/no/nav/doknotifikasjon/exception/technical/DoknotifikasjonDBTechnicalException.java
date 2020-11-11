package no.nav.doknotifikasjon.exception.technical;

public class DoknotifikasjonDBTechnicalException extends AbstractDoknotifikasjonTechnicalException {
	public DoknotifikasjonDBTechnicalException(String message, Throwable t) {
		super(message, t);
	}

	public DoknotifikasjonDBTechnicalException(String message) {
		super(message);
	}
}
