package no.nav.doknotifikasjon.consumer.digdir.krr.proxy;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalKontaktinformasjonTo {

	private Map<String, String> feil;
	private Map<String, DigitalKontaktinfo> personer;

	@Data
	@Builder
	@FieldDefaults(level = AccessLevel.PRIVATE)
	public static class DigitalKontaktinfo {
		String epostadresse;
		boolean kanVarsles;
		String mobiltelefonnummer;
		boolean reservert;
	}
}