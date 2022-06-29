package no.nav.doknotifikasjon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;

import javax.validation.constraints.NotEmpty;

@Data
@ConfigurationProperties("digdir.krr.proxy")
@Validated
@Configuration
public class DigdirKrrProxyConfig {

	@NotEmpty
	private String baseUrl;

	@NotEmpty
	private String scope;

	@Bean("digdirKrrProxyClient")
	public WebClient webClient(WebClient.Builder webClientBuilder) {
		return webClientBuilder
				.clone()
				.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}
}
