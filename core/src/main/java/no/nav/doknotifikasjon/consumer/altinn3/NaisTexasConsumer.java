package no.nav.doknotifikasjon.consumer.altinn3;

import no.nav.doknotifikasjon.config.NaisProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

@Profile("altinn3")
@Service
public class NaisTexasConsumer {
	private static final Pattern TARGET_PATTERN = Pattern.compile("api://[^.]+\\.[^.]+\\.[^.]+/\\.default");
	private final RestClient restClient;

	public NaisTexasConsumer(RestClient.Builder restClientBuilder, NaisProperties naisProperties) {
		this.restClient = restClientBuilder
			.baseUrl(naisProperties.tokenEndpoint())
			.build();
	}

	/**
	 * Maskin-til-maskin systemtoken fra Texas
	 * @param targetScope Maskin man vil autorisere mot på format api://<cluster>.<namespace>.<other-api-app-name>/.default
	 * @return Bearer token
	 */
	public String getSystemToken(String targetScope) {
		if (isBlank(targetScope) || !TARGET_PATTERN.matcher(targetScope).matches()) {
			throw new IllegalArgumentException("Ugyldig targetScope. Må være på format api://<cluster>.<namespace>.<other-api-app-name>/.default");
		}

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("identity_provider", "azuread");
		formData.add("target", targetScope);

		return requireNonNull(restClient.post()
			.contentType(APPLICATION_FORM_URLENCODED)
			.body(formData)
			.retrieve()
			.body(NaisTexasToken.class))
			.accessToken();
	}

	/**
	 * Maskinporten token fra Texas
	 *
	 * @param targetScopes Scopes man vil autorisere mot Maskinporten
	 * @return Bearer token
	 */
	public String getMaskinportenToken(String... targetScopes) {
		String formattedTargetScopes = String.join(" ", targetScopes);
		if (isBlank(formattedTargetScopes)) {
			throw new IllegalArgumentException("target scopes mot Maskinporten kan ikke være null eller blank");
		}

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("identity_provider", "maskinporten");
		formData.add("target", formattedTargetScopes);

		return requireNonNull(restClient.post()
			.contentType(APPLICATION_FORM_URLENCODED)
			.body(formData)
			.retrieve()
			.body(NaisTexasToken.class))
			.accessToken();
	}
}
