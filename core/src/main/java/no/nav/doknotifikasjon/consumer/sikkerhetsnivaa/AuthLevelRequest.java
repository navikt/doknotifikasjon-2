package no.nav.doknotifikasjon.consumer.sikkerhetsnivaa;

import lombok.Builder;
import lombok.Data;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Data
@Builder
public class AuthLevelRequest {
	private String personidentifikator;
}
