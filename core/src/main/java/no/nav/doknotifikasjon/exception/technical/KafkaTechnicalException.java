package no.nav.doknotifikasjon.exception.technical;
public class KafkaTechnicalException extends AbstractDoknotifikasjonTechnicalException {
    public KafkaTechnicalException(String s, Throwable t) {
        super(s, t);
    }

    public KafkaTechnicalException(String s) {
        super(s);
    }
}
