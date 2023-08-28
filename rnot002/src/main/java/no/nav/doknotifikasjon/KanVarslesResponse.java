package no.nav.doknotifikasjon;

import io.swagger.v3.oas.annotations.media.Schema;

public record KanVarslesResponse(
		@Schema(description = "Sier om bruker kan varsles digitalt eller ikke",
				example = "true")
		boolean kanVarsles
) {
}
