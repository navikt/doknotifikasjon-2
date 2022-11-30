package no.nav.doknotifikasjon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.doknotifikasjon.mdc.MDCUtils.handleMdc;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping("/rest/v1/notifikasjoninfo/")
@Tag(name = "hentnotifikasjonsinfo - kun for intern bruk", description = "Tjeneste for å hente informasjon om en notifikasjon")
public class Rnot001Controller {

	private final Rnot001Service rnot001Service;

	public Rnot001Controller(Rnot001Service rnot001Service) {
		this.rnot001Service = rnot001Service;
	}

	@Protected
	@GetMapping("{bestillingsId}")
	@Operation(summary = "Hent informasjon om en notifikasjon.")
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Returnerer informasjon om en notifikasjon og dens tilhørende distribusjoner.",
					content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = NotifikasjonInfoTo.class))}
			),
			@ApiResponse(
					responseCode = "401",
					description = "Ugyldig OIDC token. Denne feilen gis dersom tokenet ikke har riktig format eller er utgått.",
					content = @Content
			),
			@ApiResponse(
					responseCode = "404",
					description = "Notifikasjon ble ikke funnet i databasen.",
					content = @Content
			)
	})
	public NotifikasjonInfoTo getDistribusjonsdata(
			@Parameter(
					name = "bestillingsId",
					description = "Unik identifikasjon av bestilling",
					example = "0198d7c0-1134-49b1-9f88-94b425611337"
			)
			@PathVariable String bestillingsId) {
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
