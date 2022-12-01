package no.nav.doknotifikasjon.springdoc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER;
import static io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@ConditionalOnProperty(
		value = {"springdoc.enabled"},
		havingValue = "true"
)
@Configuration
public class Springdoc {

	@Bean
	public OpenAPI DoknotifikasjonApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Doknotifikasjon-API")
						.description("""
								Her dokumenteres REST tjenestegrensesnittet til doknotifikasjon-2.
								                      								
								Til autentisering brukes OIDC-token (JWT via OAuth 2.0) utstedt av Azure.
								
								Har du spørsmål? Kontakt oss på Slack-kanalen #team_dokumentløsninger
								"""))
				.components(
						new Components()
								.addSecuritySchemes("Authorization",
										new SecurityScheme()
												.type(HTTP)
												.scheme("bearer")
												.bearerFormat("JWT")
												.in(HEADER)
												.description("Eksempel på verdi som skal inn i Value-feltet (Bearer trengs altså ikke å oppgis): 'eyAidH...'")
												.name(AUTHORIZATION)
								)
				)
				.addSecurityItem(
						new SecurityRequirement()
								.addList("Authorization")
				);
	}
}