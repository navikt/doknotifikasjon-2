package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.doknotifikasjon.mdc.MDCUtils.handleMdc;

@Slf4j
@RestController
@RequestMapping("/rest/v1/kanvarsles/")
public class Rnot002Controller {

	private final Rnot002Service rnot002Service;

	public Rnot002Controller(Rnot002Service rnot002Service) {
		this.rnot002Service = rnot002Service;
	}

	@Protected
	@GetMapping("{personident}")
	public KanVarslesResponse getKanVarsles(@PathVariable String personident) {
		try {
			handleMdc();
			return rnot002Service.getKanVarsles(personident);
		} finally {
			MDC.clear();
		}
	}
}
