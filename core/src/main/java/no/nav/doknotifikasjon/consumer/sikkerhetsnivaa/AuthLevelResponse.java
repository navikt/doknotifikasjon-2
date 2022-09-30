package no.nav.doknotifikasjon.consumer.sikkerhetsnivaa;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthLevelResponse {
	private boolean harbruktnivaa4;
	private String personidentifikator;
}
