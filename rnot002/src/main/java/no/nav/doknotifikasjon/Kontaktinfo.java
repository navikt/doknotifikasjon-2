package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.KontaktinfoTo;
import no.nav.doknotifikasjon.domain.Epostadresse;
import no.nav.doknotifikasjon.domain.Mobiltelefonnummer;
import no.nav.doknotifikasjon.exception.functional.UgyldigEpostException;
import no.nav.doknotifikasjon.exception.functional.UgyldigMobiltelefonnummerException;

public record Kontaktinfo(
		boolean kanVarsles,
		boolean reservert,
		Epostadresse epostadresse,
		Mobiltelefonnummer mobiltelefonnummer
) {

	public static boolean personKanVarsles(Kontaktinfo kontaktinfo) {
		return kontaktinfo.kanVarsles() &&
				!kontaktinfo.reservert() &&
				(kontaktinfo.epostadresse() != null || kontaktinfo.mobiltelefonnummer() != null);
	}

	public static Kontaktinfo from(KontaktinfoTo kontaktinfoTo) {

		return new Kontaktinfo(
				kontaktinfoTo.kanVarsles(),
				kontaktinfoTo.reservert(),
				epostadresse(kontaktinfoTo.epostadresse()),
				mobiltelefonnummer(kontaktinfoTo.mobiltelefonnummer())
		);
	}

	private static Epostadresse epostadresse(String value) {
		try {
			return new Epostadresse(value);
		} catch (UgyldigEpostException e) {
			return null;
		}
	}

	private static Mobiltelefonnummer mobiltelefonnummer(String value) {
		try {
			return new Mobiltelefonnummer(value);
		} catch (UgyldigMobiltelefonnummerException e) {
			return null;
		}
	}
}
