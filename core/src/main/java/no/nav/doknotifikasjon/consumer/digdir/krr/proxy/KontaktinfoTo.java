package no.nav.doknotifikasjon.consumer.digdir.krr.proxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KontaktinfoTo(
		boolean kanVarsles,
		boolean reservert,
		String epostadresse,
		String mobiltelefonnummer
) {
	public KontaktinfoTo(boolean kanVarsles, boolean reservert, String epostadresse, String mobiltelefonnummer) {
		this.kanVarsles = kanVarsles;
		this.reservert = reservert;
		this.epostadresse = epostadresse != null ? epostadresse : "";
		this.mobiltelefonnummer = mobiltelefonnummer != null ? mobiltelefonnummer : "";
	}
}
