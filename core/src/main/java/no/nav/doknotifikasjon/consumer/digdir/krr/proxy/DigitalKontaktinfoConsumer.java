package no.nav.doknotifikasjon.consumer.digdir.krr.proxy;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.config.DigdirKrrProxyConfig;
import no.nav.doknotifikasjon.exception.functional.DigitalKontaktinformasjonFunctionalException;
import no.nav.doknotifikasjon.exception.technical.DigitalKontaktinformasjonTechnicalException;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.security.AzureToken;
import no.nav.doknotifikasjon.security.WebClientAzureAuthentication;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

import static java.lang.String.format;
import static no.nav.doknotifikasjon.constants.MDCConstants.NAV_CALL_ID;
import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;
import static no.nav.doknotifikasjon.mdc.MDCGenerate.getDefaultUuidIfNoCallIdIsSett;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_DIGDIR_KRR_PROXY_CONSUMER;

@Slf4j
@Component
public class DigitalKontaktinfoConsumer {

	private final WebClient webClient;

	public DigitalKontaktinfoConsumer(AzureToken azureToken,
									  DigdirKrrProxyConfig digdirKrrProxyConfig,
									  @Qualifier("digdirKrrProxyClient") WebClient webClient) {
		this.webClient = webClient
				.mutate()
				.filter(new WebClientAzureAuthentication(azureToken, digdirKrrProxyConfig.getScope()))
				.build();
	}

	@Metrics(value = DOK_DIGDIR_KRR_PROXY_CONSUMER, createErrorMetric = true, errorMetricInclude = DigitalKontaktinformasjonTechnicalException.class)
	@Retryable(retryFor = DigitalKontaktinformasjonTechnicalException.class, backoff = @Backoff(delay = DELAY_LONG))
	public DigitalKontaktinformasjonTo hentDigitalKontaktinfo(final String personidentifikator) {

		var fnrTrimmed = personidentifikator.trim();
		var body = PostPersonerRequest.builder().personidenter(List.of(fnrTrimmed)).build();

		return webClient.post()
				.uri("/rest/v1/personer")
				.header(NAV_CALL_ID, getDefaultUuidIfNoCallIdIsSett())
				.body(Mono.just(body), PostPersonerRequest.class)
				.retrieve()
				.bodyToMono(DigitalKontaktinformasjonTo.class)
				.onErrorMap(this::mapError)
				.block();
	}

	@Metrics(value = DOK_DIGDIR_KRR_PROXY_CONSUMER, createErrorMetric = true,
			errorMetricInclude = DigitalKontaktinformasjonTechnicalException.class,
			logExceptions = false)
	@Retryable(retryFor = DigitalKontaktinformasjonTechnicalException.class)
	public KontaktinfoTo hentDigitalKontaktinfoForPerson(final String personidentifikator) {

		var fnrTrimmed = personidentifikator.trim();

		return webClient.get()
				.uri("/rest/v1/person")
				.header(NAV_CALL_ID, getDefaultUuidIfNoCallIdIsSett())
				.header("Nav-Personident", fnrTrimmed)
				.retrieve()
				.bodyToMono(KontaktinfoTo.class)
				.onErrorMap(this::mapError)
				.block();
	}

	private Throwable mapError(Throwable error) {
		if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
			return new DigitalKontaktinformasjonFunctionalException(format("Kall mot Digdir-krr-proxy feilet med status=%s, feilmelding=%s",
					response.getStatusCode(), response.getMessage()), error, (HttpStatus) response.getStatusCode()
			);
		} else {
			return new DigitalKontaktinformasjonTechnicalException(format("Kall mot Digdir-krr-proxy feilet med feilmelding=%s",
					error.getMessage()), error);
		}
	}

}