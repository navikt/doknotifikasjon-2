package no.nav.doknotifikasjon.consumer.altinn3;

import no.nav.doknotifikasjon.config.properties.Altinn3Props;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static java.lang.String.format;
import static no.nav.doknotifikasjon.config.LokalCacheConfig.ALTINN_TOKEN_CACHE;

@Profile("altinn3")
@Component
public class Altinn3TokenExchangeConsumer {

	private final NaisTexasConsumer naisTexasConsumer;
	private final RestClient restClient;

	public Altinn3TokenExchangeConsumer(NaisTexasConsumer naisTexasConsumer, RestClient.Builder restClientBuilder, Altinn3Props altinn3Props) {
		this.naisTexasConsumer = naisTexasConsumer;
		this.restClient = restClientBuilder
			.baseUrl(altinn3Props.altinnTokenExchangeUri())
			.build();
	}

	@Cacheable(ALTINN_TOKEN_CACHE)
	public String getAltinnToken(String... scopes) {
		try {
			return restClient.get()
				.header("Authorization", "Bearer " + naisTexasConsumer.getMaskinportenToken(scopes))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.toEntity(String.class)
				.getBody();
		} catch (RestClientResponseException e) {
			throw new AltinnTechnicalException(format("Teknisk feil i kall mot Altinn ved exchange av maskinporten-token mot altinn-token. %s", e.getStatusCode()), e);
		} catch (Exception e) {
			throw new AltinnTechnicalException("Ukjent teknisk feil ved kall mot Altinn ved exchange av maskinporten-token mot altinn-token.", e);
		}
	}
}
