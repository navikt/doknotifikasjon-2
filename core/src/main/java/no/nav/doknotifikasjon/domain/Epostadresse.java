package no.nav.doknotifikasjon.domain;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.UgyldigEpostException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

@Slf4j
public record Epostadresse(String epostadresse) {

	private static final int MAKS_LENGDE_EPOST_BRUKERNAVN = 50;
	private static final EmailValidator emailValidator = EmailValidator.getInstance();

	public Epostadresse(String epostadresse) {
		if (!isEpostValid(epostadresse)) {
			log.warn("Validering av e-postadresse feilet");
			throw new UgyldigEpostException("Formatet på e-postadressen er ikke gyldig");
		} else {
			this.epostadresse = epostadresse;
		}
	}

	private boolean isEpostValid(String epost) {
		int brukernavnlengde = StringUtils.substringBefore(epost, "@").length();
		if (brukernavnlengde > MAKS_LENGDE_EPOST_BRUKERNAVN) {
			return false;
		}

		return emailValidator.isValid(epost);
	}

}