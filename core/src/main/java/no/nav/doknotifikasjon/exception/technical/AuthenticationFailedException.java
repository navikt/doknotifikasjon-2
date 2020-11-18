package no.nav.doknotifikasjon.exception.technical;

public class AuthenticationFailedException extends AbstractDoknotifikasjonTechnicalException {
	public AuthenticationFailedException(String s, Throwable t) {
		super(s, t);
	}
}
