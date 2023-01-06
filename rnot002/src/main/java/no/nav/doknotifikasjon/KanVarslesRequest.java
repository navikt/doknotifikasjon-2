package no.nav.doknotifikasjon;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KanVarslesRequest {
	private static final String PERSONIDENT_REGEX = "^\\d{11}$";

	@Schema(description = "Personident til personen som skal vurderes",
			example = "12345678901")
	@Pattern(regexp = PERSONIDENT_REGEX)
	@NotNull
	String personident;
}
