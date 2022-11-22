package no.nav.doknotifikasjon.consumer.digdir.krr.proxy;


import java.util.Map;

public record DigitalKontaktinformasjonTo(
		Map<String, String> feil,
		Map<String, DigitalKontaktinfo> personer
) {
	public record DigitalKontaktinfo(
			String epostadresse,
			boolean kanVarsles,
			String mobiltelefonnummer,
			boolean reservert
	) {
	}
}