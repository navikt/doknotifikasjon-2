package no.nav.doknotifikasjon.knot002.consumer;

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.mockito.ArgumentMatcher;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class PrintStreamMatcher implements ArgumentMatcher<byte[]> {

	private final List<String> left;

	public PrintStreamMatcher(List<String> stringsToMatch) {
		this.left = stringsToMatch;
	}

	@Override
	public boolean matches(byte[] right) {
		String string = new String(right, StandardCharsets.UTF_8);
		return left.stream().allMatch(string::contains);
	}
}