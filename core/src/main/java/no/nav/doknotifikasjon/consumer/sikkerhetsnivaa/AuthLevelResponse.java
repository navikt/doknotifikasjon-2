package no.nav.doknotifikasjon.consumer.sikkerhetsnivaa;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Data
@AllArgsConstructor
public class AuthLevelResponse {
	private boolean harBruktNivaa4;
	private String personidentifikator;
}
