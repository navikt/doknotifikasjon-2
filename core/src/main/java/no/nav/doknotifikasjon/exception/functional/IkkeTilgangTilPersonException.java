package no.nav.doknotifikasjon.exception.functional;

import no.nav.doknotifikasjon.exception.functional.AbstractDoknotifikasjonFunctionalException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@ResponseStatus(FORBIDDEN)
public class IkkeTilgangTilPersonException extends AbstractDoknotifikasjonFunctionalException {

	public IkkeTilgangTilPersonException(String message) {
		super(message);
	}
}
