package no.nav.doknotifikasjon.domain;

import no.nav.doknotifikasjon.exception.functional.UgyldigMobiltelefonnummerException;

import java.util.regex.Pattern;

public record Mobiltelefonnummer(String mobiltelefonnummer) {
	private static final String REGEX_NORSK_MOBILTELEFONNUMMER = "^((0047)?|(\\+47)?)[4|9]\\d{7}$";
	private static final String REGEX_INTERNASJONALT_TELEFONNUMMER = "^(?:00|\\+)(?!47)[-0-9]+";
	private static final Pattern PATTERN_NORSK_MOBILTELEFONNUMMER = Pattern.compile(REGEX_NORSK_MOBILTELEFONNUMMER);
	private static final Pattern PATTERN_INTERNASJONALT_TELEFONNUMMER = Pattern.compile(REGEX_INTERNASJONALT_TELEFONNUMMER);
	private static final int INTERNASJONALT_MOBILNUMMER_MAX_LENGTH = 20;
	private static final int INTERNASJONALT_MOBILNUMMER_MIN_LENGTH = 8;

	public Mobiltelefonnummer(String mobiltelefonnummer) {
		if (!isMobilnummerValid(mobiltelefonnummer)) {
			throw new UgyldigMobiltelefonnummerException("Formatet på mobiltelefonnummeret er ikke gyldig");
		} else {
			this.mobiltelefonnummer = mobiltelefonnummer;
		}
	}

	private boolean isMobilnummerValid(String mobilnummer) {
		return PATTERN_NORSK_MOBILTELEFONNUMMER.matcher(mobilnummer).matches() ||
				(isValidLength(mobilnummer) && PATTERN_INTERNASJONALT_TELEFONNUMMER.matcher(mobilnummer).matches());
	}

	private boolean isValidLength(String mobilnummer) {
		return mobilnummer.length() <= INTERNASJONALT_MOBILNUMMER_MAX_LENGTH &&
				mobilnummer.length() >= INTERNASJONALT_MOBILNUMMER_MIN_LENGTH;
	}
}