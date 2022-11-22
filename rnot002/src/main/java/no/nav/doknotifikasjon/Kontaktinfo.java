package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.exception.functional.DigitalKontaktinformasjonFunctionalException;
import no.nav.doknotifikasjon.exception.functional.UgyldigEpostException;
import no.nav.doknotifikasjon.exception.functional.UgyldigMobiltelefonnummerException;

public record Kontaktinfo(
		String feil,
		Epostadresse epostadresse,
		boolean kanVarsles,
		Mobiltelefonnummer mobiltelefonnummer,
		boolean reservert
) {
	public Kontaktinfo(String feil, String epostadresse, boolean kanVarsles, String mobiltelefonnummer, boolean reservert) {
		this(feil, new Epostadresse(epostadresse), kanVarsles, new Mobiltelefonnummer(mobiltelefonnummer), reservert);
	}

	public record Epostadresse(String epostadresse) {
		public Epostadresse(String epostadresse) {
			if (!KontaktinfoValidator.isEpostValid(epostadresse)) {
				throw new UgyldigEpostException("Formatet på e-postadressen er ikke gyldig");
			} else {
				this.epostadresse = epostadresse;
			}
		}
	}

	public record Mobiltelefonnummer(String mobiltelefonnummer) {
		public Mobiltelefonnummer(String mobiltelefonnummer) {
			if (!KontaktinfoValidator.isMobilnummerValid(mobiltelefonnummer)) {
				throw new UgyldigMobiltelefonnummerException("Formatet på mobiltelefonnummeret er ikke gyldig");
			} else {
				this.mobiltelefonnummer = mobiltelefonnummer;
			}
		}
	}
}
