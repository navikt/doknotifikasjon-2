package no.nav.doknotifikasjon.exception.technical;

public class DoknotifikasjonDBTechnicalException extends AbstractDoknotifikasjonTechnicalException {
	public DoknotifikasjonDBTechnicalException(String s, Throwable t) {
		super(s, t);
	}

	public DoknotifikasjonDBTechnicalException(String s) {
		super(s);
	}
}
