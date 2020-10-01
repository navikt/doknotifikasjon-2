package no.nav.doknotifikasjon.consumer.dkif;


import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class DigitalKontaktinformasjonTo {

	private Map<String, Feil> feil;
	private Map<String, DigitalKontaktinfo> kontaktinfo;

	@Data
	@Builder
	private static class Feil {
		private String melding;
	}

	@Data
	@Builder
	public static class DigitalKontaktinfo {
		private String epostadresse;
		private boolean kanVarsles;
		private String mobiltelefonnummer;
		private boolean reservert;
	}

}