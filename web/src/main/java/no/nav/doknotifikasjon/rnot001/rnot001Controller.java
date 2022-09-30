package no.nav.doknotifikasjon.rnot001;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@RestController
@RequestMapping("/rest/v1/notifikasjoninfo/")
public class rnot001Controller {

	private final Rnot001Service rnot001Service;

	public rnot001Controller(Rnot001Service rnot001Service) {
		this.rnot001Service = rnot001Service;
	}

	@Protected
	@GetMapping("{bestillingsId}")
	public NotifikasjonInfoTo getDistribusjonsdata(@PathVariable String bestillingsId) {
		if (isBlank(bestillingsId)) {
			throw new DoknotifikasjonDistribusjonIkkeFunnetException("Det finnes ingen distribusjon med id=" + bestillingsId);
		}

		return rnot001Service.getNotifikasjonInfo(bestillingsId);
	}
}
