package no.nav.doknotifikasjon.config;

import no.nav.doknotifikasjon.config.properties.Altinn3Props;
import no.nav.doknotifikasjon.consumer.altinn3.MaskinportenAltinn3RequestInterceptor;
import no.nav.doknotifikasjon.consumer.altinn3.Altinn3TokenExchangeConsumer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static java.time.Duration.ofSeconds;


@Configuration
@Profile("altinn3")
@EnableConfigurationProperties({Altinn3Props.class, NaisProperties.class})
public class Altinn3VarselConfiguration {

	@Bean
	public RestClient naisTexasMaskinportenAuthenticatedRestClient(RestClient.Builder restClientBuilder, Altinn3TokenExchangeConsumer altinn3TokenExchangeConsumer) {
		return restClientBuilder
			.requestInterceptor(new MaskinportenAltinn3RequestInterceptor(altinn3TokenExchangeConsumer))
			.requestFactory(jdkClientHttpRequestFactory())
			.build();
	}

	private static JdkClientHttpRequestFactory jdkClientHttpRequestFactory() {
		return ClientHttpRequestFactoryBuilder.jdk()
			.withCustomizer(jdkClientHttpRequestFactory ->
				jdkClientHttpRequestFactory.setReadTimeout(ofSeconds(20))
			)
			.build();
	}
}
