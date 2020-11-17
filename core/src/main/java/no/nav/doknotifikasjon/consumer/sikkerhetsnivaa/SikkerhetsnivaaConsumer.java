package no.nav.doknotifikasjon.consumer.sikkerhetsnivaa;

import no.nav.doknotifikasjon.exception.functional.SikkerhetsnivaaFunctionalException;
import no.nav.doknotifikasjon.exception.technical.SikkerhetsnivaaTechnicalException;
import no.nav.doknotifikasjon.metrics.Metrics;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.Duration;

import static no.nav.doknotifikasjon.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_SIKKERHETSNIVAA_CONSUMER;
import static no.nav.doknotifikasjon.metrics.MetricTags.HENT_AUTH_LEVEL;
import static no.nav.doknotifikasjon.metrics.MetricTags.PROCESS_NAME;

@Component
public class SikkerhetsnivaaConsumer {
	private final RestTemplate restTemplate;
	private final String sikkerhetsnivaaUrl;

	@Inject
	public SikkerhetsnivaaConsumer(@Value("${hentpaaloggingsnivaa_v1_url}") String sikkerhetsnivaaUrl,
								   RestTemplateBuilder restTemplateBuilder) {
		this.sikkerhetsnivaaUrl = sikkerhetsnivaaUrl;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@Metrics(value = DOK_SIKKERHETSNIVAA_CONSUMER, extraTags = {PROCESS_NAME, HENT_AUTH_LEVEL})
	public AuthLevelResponse lookupAuthLevel(final String personIdent) {
		try {
			HttpEntity<AuthLevelRequest> request = new HttpEntity<>(AuthLevelRequest.builder().personidentifikator(personIdent).build());
			ResponseEntity<AuthLevelResponse> response = restTemplate.postForEntity(sikkerhetsnivaaUrl, request, AuthLevelResponse.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			switch (e.getStatusCode()) {
				case UNAUTHORIZED:
					throw new SikkerhetsnivaaFunctionalException("Difi IdPorten avviste accesstoken.", e);
				case FORBIDDEN:
					throw new SikkerhetsnivaaFunctionalException("Bruker er ikke registrert som ID-porten bruker. Feilmelding: " + e.getResponseBodyAsString(), e);
				default:
					throw new SikkerhetsnivaaFunctionalException("Difi IdPorten feilet med ukjent funksjonell feil", e);
			}
		} catch (HttpServerErrorException e) {
			throw new SikkerhetsnivaaTechnicalException("Difi IdPorten server error. CallId=" + MDC.get(MDC_CALL_ID), e);
		}
	}
}
