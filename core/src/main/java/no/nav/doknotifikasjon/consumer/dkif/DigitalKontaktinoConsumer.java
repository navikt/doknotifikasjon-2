package no.nav.doknotifikasjon.consumer.dkif;

import static java.lang.String.format;
import static no.nav.doknotifikasjon.constants.DomainConstants.APP_NAME;
import static no.nav.doknotifikasjon.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doknotifikasjon.constants.MDCConstants.NAV_CALL_ID;
import static no.nav.doknotifikasjon.constants.MDCConstants.NAV_CONSUMER_ID;
import static no.nav.doknotifikasjon.constants.MDCConstants.NAV_PERSONIDENTER;
import static no.nav.doknotifikasjon.metrics.MetricLabels.DOK_CONSUMER;
import static no.nav.doknotifikasjon.metrics.MetricLabels.PROCESS_NAME;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DigitalKontaktinformasjonFunctionalException;
import no.nav.doknotifikasjon.exception.technical.DigitalKontaktinformasjonTechnicalException;
import no.nav.doknotifikasjon.metrics.Metrics;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.Duration;

@Slf4j
@Component
public class DigitalKontaktinoConsumer implements DigitalKontaktinformasjon {

	private final String dkifUrl;
	private final RestTemplate restTemplate;

	private static final String HENT_DIGITAL_KONTAKTINFORMASJON = "hentDigitalKontaktinformasjon";

	@Inject
	public DigitalKontaktinoConsumer(@Value("dkif_url") String dkifUrl, RestTemplateBuilder restTemplateBuilder) {
		this.dkifUrl = dkifUrl;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@Retryable(include = DigitalKontaktinformasjonTechnicalException.class, maxAttempts = 5, backoff = @Backoff(delay = 200))
	@Metrics(value = DOK_CONSUMER, extraTags = {PROCESS_NAME, HENT_DIGITAL_KONTAKTINFORMASJON}, percentiles = {0.5, 0.95}, histogram = true)
	public DigitalKontaktinformasjonTo.DigitalKontaktinfo hentDigitalKontaktinfo(final String personidentifikator) {
		HttpHeaders headers = createHeaders();
		String fnr_trimmed = personidentifikator.trim();
		headers.add(NAV_PERSONIDENTER, fnr_trimmed);

		try {
			DigitalKontaktinformasjonTo response = restTemplate.exchange(dkifUrl + "/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false",
					HttpMethod.GET, new HttpEntity<>(headers), DigitalKontaktinformasjonTo.class).getBody();
			if (isDigitalkontaktinformasjonValid(response, fnr_trimmed)) {
				return response.getKontaktinfo().get(fnr_trimmed);
			} else {
				//todo: skriv notifikasjon-feilet hendelse til kafka-topic dokeksternnotifikasjon-status og avslutt behandlingen
				return null; //??
			}
		} catch (HttpClientErrorException e) {
			throw new DigitalKontaktinformasjonFunctionalException(format("Funksjonell feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s", e
					.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new DigitalKontaktinformasjonTechnicalException(format("Teknisk feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s", e
					.getMessage()), e);
		}
	}

	private boolean isDigitalkontaktinformasjonValid(DigitalKontaktinformasjonTo response, String fnr) {
		if (isResponsValid(response, fnr)) {
			DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinfo = response.getKontaktinfo().get(fnr);
			return kontaktinfo.isKanVarsles() && !kontaktinfo.isReservert() && isEpostOrSmsValid(kontaktinfo);
		} else {
			return false;
		}
	}

	private boolean isResponsValid(DigitalKontaktinformasjonTo response, String fnr) {
		return response != null && response.getKontaktinfo() != null && response.getKontaktinfo().get(fnr) != null;
	}

	private boolean isEpostOrSmsValid(DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinfo) {
		return kontaktinfo.getEpostadresse() != null || kontaktinfo.getMobiltelefonnummer() != null;
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
//		headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + stsRestConsumer.getOidcToken());
		headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, MDC.get(MDC_CALL_ID));
		return headers;
	}
}