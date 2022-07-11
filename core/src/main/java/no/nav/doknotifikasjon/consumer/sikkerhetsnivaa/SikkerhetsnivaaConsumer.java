package no.nav.doknotifikasjon.consumer.sikkerhetsnivaa;

import no.nav.doknotifikasjon.exception.functional.SikkerhetsnivaaFunctionalException;
import no.nav.doknotifikasjon.exception.technical.SikkerhetsnivaaTechnicalException;
import no.nav.doknotifikasjon.metrics.Metrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;
import static no.nav.doknotifikasjon.constants.RetryConstants.RETRIES;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_SIKKERHETSNIVAA_CONSUMER;

@Component
public class SikkerhetsnivaaConsumer {

	private final RestTemplate restTemplate;
	private final String sikkerhetsnivaaUrl;

	@Autowired
	public SikkerhetsnivaaConsumer(@Value("${sikkerhetsnivaa_v1_url}") String sikkerhetsnivaaUrl,
								   RestTemplateBuilder restTemplateBuilder) {
		this.sikkerhetsnivaaUrl = sikkerhetsnivaaUrl;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@Metrics(value = DOK_SIKKERHETSNIVAA_CONSUMER, createErrorMetric = true)
	@Retryable(include = SikkerhetsnivaaTechnicalException.class, maxAttempts = RETRIES, backoff = @Backoff(delay = DELAY_LONG))
	public AuthLevelResponse lookupAuthLevel(final String personIdent) {
		try {
			HttpEntity<AuthLevelRequest> request = new HttpEntity<>(AuthLevelRequest.builder().personidentifikator(personIdent).build());
			ResponseEntity<AuthLevelResponse> response = restTemplate.postForEntity(sikkerhetsnivaaUrl, request, AuthLevelResponse.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			switch (e.getStatusCode()) {
				case UNAUTHORIZED -> throw new SikkerhetsnivaaFunctionalException(String.format("Difi IdPorten avviste accesstoken. Feilmelding: %s", e.getMessage()), e);
				case FORBIDDEN -> throw new SikkerhetsnivaaFunctionalException(String.format("Bruker er ikke registrert som ID-porten bruker. Feilmelding: %s", e.getMessage()), e);
				default -> throw new SikkerhetsnivaaFunctionalException(String.format("Difi IdPorten feilet med ukjent funksjonell feil. Feilmelding: %s", e.getMessage()), e);
			}
		} catch (HttpServerErrorException e) {
			throw new SikkerhetsnivaaTechnicalException(String.format("Teknisk feil mot Difi IdPorten. Feilmelding: %s", e.getMessage()), e);
		}
	}
}
