package no.nav.doknotifikasjon;

import org.springframework.stereotype.Component;

@Component
public class Knot004Validator {

	public void shouldValidateInput(DoknotifikasjonStatusDto doknotifikasjonStatusDto){
		if(doknotifikasjonStatusDto)
	}

	private boolean isNullOrEmpty(String field){
		return field == null || field.isEmpty();
	}
}
