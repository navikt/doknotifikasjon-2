package no.nav.doknotifikasjon.consumer.altinn3;

import no.nav.doknotifikasjon.config.properties.Altinn3Props;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static java.lang.String.format;
import static no.nav.doknotifikasjon.config.LokalCacheConfig.ALTINN_TOKEN_CACHE;

@Component
public class Altinn3TokenExchangeConsumer {

	private final NaisTexasConsumer naisTexasConsumer;
	private final JsonMapper jsonMapper;
	private final RestClient restClient;

	public Altinn3TokenExchangeConsumer(NaisTexasConsumer naisTexasConsumer, JsonMapper jsonMapper, RestClient.Builder restClientBuilder, Altinn3Props altinn3Props) {
		this.naisTexasConsumer = naisTexasConsumer;
		this.jsonMapper = jsonMapper;
		this.restClient = restClientBuilder
			.baseUrl(altinn3Props.altinnTokenExchangeUri())
			.build();
	}

	@Cacheable(ALTINN_TOKEN_CACHE)
	public String getAltinnToken(String... scopes) {
		try {
			String response = restClient.get()
				.header("Authorization", "Bearer " + naisTexasConsumer.getMaskinportenToken(scopes))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(String.class);

			return jsonMapper.readValue(response, String.class);
		} catch (JacksonException e) {
			throw new AltinnTechnicalException(format("Teknisk feil i kall mot Altinn ved exchange av maskinporten-token mot altinn-token. Kunne ikke parse token: %s", e.getMessage()), e);
		} catch (RestClientResponseException e) {
			throw new AltinnTechnicalException(format("Teknisk feil i kall mot Altinn ved exchange av maskinporten-token mot altinn-token. %s", e.getStatusCode()), e);
		} catch (RestClientException e) {
			if (e.getCause() instanceof HttpMessageNotReadableException f) {
				throw new AltinnTechnicalException(format("Teknisk feil i kall mot Altinn ved exchange av maskinporten-token mot altinn-token. Kunne ikke parse token: %s", f.getMessage()), f);
			}
			throw new AltinnTechnicalException(format("Teknisk feil i kall mot Altinn ved exchange av maskinporten-token mot altinn-token. %s", e.getMessage()), e);
		} catch (Exception e) {
			throw new AltinnTechnicalException("Ukjent teknisk feil ved kall mot Altinn ved exchange av maskinporten-token mot altinn-token.", e);
		}
	}
}
