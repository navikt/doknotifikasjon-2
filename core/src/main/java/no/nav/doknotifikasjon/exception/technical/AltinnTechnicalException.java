package no.nav.doknotifikasjon.exception.technical;

public class AltinnTechnicalException extends AbstractDoknotifikasjonTechnicalException {
	public AltinnTechnicalException(String s, Throwable t) {
		super(s, t);
	}

	public AltinnTechnicalException(String s) {
		super(s);
	}
}
