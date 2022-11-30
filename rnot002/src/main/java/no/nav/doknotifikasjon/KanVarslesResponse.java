package no.nav.doknotifikasjon;

import io.swagger.v3.oas.annotations.media.Schema;

public record KanVarslesResponse(
		@Schema(description = "Sier om bruker kan varsles digitalt eller ikke",
				example = "true")
		boolean kanVarsles,
		@Schema(description = "Settes til 4 dersom bruker har brukt nivå 4-pålogging siste 18 måneder. 3 hvis ikke.",
				allowableValues = {"3", "4"},
				type = "integer",
				example = "4")
		int sikkerhetsnivaa
) {
}
