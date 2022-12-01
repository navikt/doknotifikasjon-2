package no.nav.doknotifikasjon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.ConstraintViolationException;

import static no.nav.doknotifikasjon.mdc.MDCUtils.handleMdc;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping("/rest/v1/kanvarsles")
@Tag(name = "kanvarsles", description = "Tjeneste for å sjekke om en person kan varsles digitalt")
public class Rnot002Controller {

	private final Rnot002Service rnot002Service;

	public Rnot002Controller(Rnot002Service rnot002Service) {
		this.rnot002Service = rnot002Service;
	}

	@Protected
	@PostMapping
	@Operation(summary = "Få informasjon om en person kan varsles digitalt.")
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Returnerer informasjon om en person kan varsles med tilhørende sikkerhetsnivå.",
					content = {@Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = KanVarslesResponse.class))}
			),
			@ApiResponse(
					responseCode = "400",
					description = "Ugyldig personident mottatt. Denne feilen gis dersom personidenten ikke har nøyaktig 11 siffer.",
					content = {@Content(schema = @Schema(example = "Personident må bestå av nøyaktig 11 siffer."))}
			),
			@ApiResponse(
					responseCode = "401",
					description = "Ugyldig OIDC token. Denne feilen gis dersom tokenet ikke har riktig format eller er utgått.",
					content = @Content
			)
	})
	public KanVarslesResponse getKanVarsles(@Validated @RequestBody KanVarslesRequest request) {
		try {
			handleMdc();
			return rnot002Service.getKanVarsles(request.getPersonident());
		} finally {
			MDC.clear();
		}
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<String> invalidPersonidentHandler() {
		return new ResponseEntity<>("Personident må bestå av nøyaktig 11 siffer.", BAD_REQUEST);
	}
}
