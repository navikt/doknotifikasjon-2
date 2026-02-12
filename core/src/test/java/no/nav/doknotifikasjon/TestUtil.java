package no.nav.doknotifikasjon;

import org.springframework.http.HttpStatus;

public class TestUtil {
	private static final String ERROR_TITLE = "NOT-00001";
	private static final String ERROR_MESSAGE = "Ugyldig norsk mobiltelefonnummer.";

	public static String createErrorMessage(HttpStatus status) {
		return String.format("instance=string, status=%d, errorType=%s, errorTitle=%s, errorMessage=%s", status.value(), ERROR_TITLE, ERROR_TITLE, ERROR_MESSAGE);
	}
}
