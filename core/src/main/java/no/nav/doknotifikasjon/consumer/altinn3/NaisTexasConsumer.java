package no.nav.doknotifikasjon.consumer.altinn3;

import no.nav.doknotifikasjon.config.NaisProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

@Service
public class NaisTexasConsumer {

	private final RestClient restClient;

	public NaisTexasConsumer(RestClient.Builder restClientBuilder, NaisProperties naisProperties) {
		this.restClient = restClientBuilder
			.baseUrl(naisProperties.tokenEndpoint())
			.build();
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
