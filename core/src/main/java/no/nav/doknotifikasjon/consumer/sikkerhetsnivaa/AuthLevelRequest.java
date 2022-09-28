package no.nav.doknotifikasjon.consumer.sikkerhetsnivaa;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthLevelRequest {
	private String personidentifikator;
}
