package no.nav.doknotifikasjon.exception.technical;

public class AbstractDoknotifikasjonTechnicalException extends RuntimeException {

    public AbstractDoknotifikasjonTechnicalException(String message) {
        super(message);
    }

    public AbstractDoknotifikasjonTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
