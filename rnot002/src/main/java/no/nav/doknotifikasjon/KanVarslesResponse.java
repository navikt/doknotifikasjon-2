package no.nav.doknotifikasjon;

public record KanVarslesResponse(
		boolean kanVarsles,
		int sikkerhetsnivaa
) {
}
