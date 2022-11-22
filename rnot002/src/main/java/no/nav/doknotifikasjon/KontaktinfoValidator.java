package no.nav.doknotifikasjon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.regex.Pattern;

// Validering av epost og telefonnummer gjort i samsvar med Difi sin begrepskatalog for felleskomponenter
public class KontaktinfoValidator {

	private static final String REGEX_NORSK_MOBILTELEFONNUMMER = "^((0047)?|(\\+47)?)[4|9]\\d{7}$";
	private static final String REGEX_INTERNASJONALT_TELEFONNUMMER = "^((00|\\+)(?!47))[-0-9]{8,20}$";
	private static final Pattern PATTERN_NORSK_MOBILTELEFONNUMMER = Pattern.compile(REGEX_NORSK_MOBILTELEFONNUMMER);
	private static final Pattern PATTERN_INTERNASJONALT_TELEFONNUMMER = Pattern.compile(REGEX_INTERNASJONALT_TELEFONNUMMER);
	private static final int MAKS_LENGDE_EPOST_BRUKERNAVN = 50;

	public static boolean isEpostValid(String epost) {
		int brukernavnlengde = StringUtils.substringBefore(epost, "@").length();
		if (brukernavnlengde > MAKS_LENGDE_EPOST_BRUKERNAVN) {
			return false;
		}

		return EmailValidator.getInstance().isValid(epost);
	}

	public static boolean isMobilnummerValid(String mobilnummer) {
		return PATTERN_NORSK_MOBILTELEFONNUMMER.matcher(mobilnummer).matches() ||
		PATTERN_INTERNASJONALT_TELEFONNUMMER.matcher(mobilnummer).matches();
	}
}
