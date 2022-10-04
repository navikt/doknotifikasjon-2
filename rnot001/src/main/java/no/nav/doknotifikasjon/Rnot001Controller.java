package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.doknotifikasjon.utils.MDCUtils.handleMdc;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@RestController
@RequestMapping("/rest/v1/notifikasjoninfo/")
public class Rnot001Controller {

	private final Rnot001Service rnot001Service;

	public Rnot001Controller(Rnot001Service rnot001Service) {
		this.rnot001Service = rnot001Service;
	}

	@Protected
	@GetMapping("{bestillingsId}")
	public NotifikasjonInfoTo getDistribusjonsdata(@PathVariable String bestillingsId) {
		try {
			handleMdc();
			if (isBlank(bestillingsId)) {
				throw new DoknotifikasjonDistribusjonIkkeFunnetException("Ugyldig bestillingsId for distribusjon oppgitt. BestillingsId kan ikke være tom");
			}
			return rnot001Service.getNotifikasjonInfo(bestillingsId);
		} finally {
			MDC.clear();
		}
	}
}
