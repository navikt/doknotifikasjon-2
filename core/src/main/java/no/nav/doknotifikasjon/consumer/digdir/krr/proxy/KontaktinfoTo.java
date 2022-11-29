package no.nav.doknotifikasjon.consumer.digdir.krr.proxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KontaktinfoTo(
		boolean kanVarsles,
		boolean reservert,
		String epostadresse,
		String mobiltelefonnummer
) {
}
