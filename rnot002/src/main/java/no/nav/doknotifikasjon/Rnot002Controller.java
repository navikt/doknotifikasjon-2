package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.ConstraintViolationException;
import javax.validation.constraints.Pattern;

import static no.nav.doknotifikasjon.mdc.MDCUtils.handleMdc;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestController
@RequestMapping("/rest/v1/kanvarsles/")
@Validated
public class Rnot002Controller {

	private static final String PERSONIDENT_REGEX = "^\\d{11}$";
	private final Rnot002Service rnot002Service;

	public Rnot002Controller(Rnot002Service rnot002Service) {
		this.rnot002Service = rnot002Service;
	}

	@Protected
	@GetMapping("{personident}")
	public KanVarslesResponse getKanVarsles(@Pattern(regexp = PERSONIDENT_REGEX) @PathVariable String personident) {
		try {
			handleMdc();
			return rnot002Service.getKanVarsles(personident);
		} finally {
			MDC.clear();
		}
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<String> invalidPersonidentHandler() {
		return new ResponseEntity<>("Personident må bestå av nøyaktig 11 siffer.", BAD_REQUEST);
	}
}
