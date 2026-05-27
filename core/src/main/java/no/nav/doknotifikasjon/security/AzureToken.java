package no.nav.doknotifikasjon.security;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.config.AzureConfig;
import no.nav.doknotifikasjon.exception.functional.AbstractDoknotifikasjonFunctionalException;
import no.nav.doknotifikasjon.exception.functional.AzureTokenException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

import static java.lang.String.format;
import static no.nav.doknotifikasjon.config.LokalCacheConfig.AZURE_TOKEN_CACHE;

@Slf4j
@Component
public class AzureToken {

	private final AzureConfig azureConfig;
	private final JsonMapper jsonMapper;
	private final WebClient webClient;

	public AzureToken(AzureConfig azureConfig,
					  JsonMapper jsonMapper,
					  @Qualifier("azureClient") WebClient webClient) {
		this.azureConfig = azureConfig;
		this.jsonMapper = jsonMapper;
		this.webClient = webClient;
	}

	@Retryable(includes = AbstractDoknotifikasjonFunctionalException.class)
	@Cacheable(AZURE_TOKEN_CACHE)
	public String accessToken(String scope) {
		return fetchAccessToken(scope);
	}

	private String fetchAccessToken(String scope) {

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("client_id", azureConfig.getAppClientId());
		formData.add("client_secret", azureConfig.getAppClientSecret());
		formData.add("grant_type", "client_credentials");
		formData.add("scope", scope);

		String responseJson = webClient.post()
				.body(BodyInserters.fromFormData(formData))
				.retrieve()
				.bodyToMono(String.class)
				.onErrorMap(this::mapError)
				.block();

		try {
			Map<String, Object> tokenData = jsonMapper.readValue(responseJson, Map.class);
			return (String) tokenData.get("access_token");
		} catch (JacksonException | ClassCastException e) {
			throw new AzureTokenException(format("Klarte ikke parse token fra Azure. Feilmelding=%s", e.getMessage()), e);
		}
	}

	private Throwable mapError(Throwable error) {
		if (error instanceof WebClientResponseException response && response.getStatusCode().is4xxClientError()) {
			return new AzureTokenException(format("Klarte ikke hente token fra Azure. Feilet med statuskode=%s Feilmelding=%s",
					response.getStatusCode(), response.getMessage()), error);
		} else {
			return new AzureTokenException(format("Kall mot Azure feilet med feilmelding=%s",
					error.getMessage()), error);
		}
	}
}
