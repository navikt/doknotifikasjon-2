package no.nav.doknotifikasjon.consumer.dkif;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.consumer.sts.StsRestConsumer;
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

import static java.lang.String.format;
import static no.nav.doknotifikasjon.constants.DomainConstants.APP_NAME;
import static no.nav.doknotifikasjon.constants.DomainConstants.BEARER_PREFIX;
import static no.nav.doknotifikasjon.constants.MDCConstants.*;
import static no.nav.doknotifikasjon.metrics.MetricLabels.DOK_CONSUMER;
import static no.nav.doknotifikasjon.metrics.MetricLabels.PROCESS_NAME;

@Slf4j
@Component
public class DigitalKontaktinfoConsumer implements DigitalKontaktinformasjon {

    private final String dkifUrl;
    private final RestTemplate restTemplate;
    private final StsRestConsumer stsRestConsumer;

    private static final String HENT_DIGITAL_KONTAKTINFORMASJON = "hentDigitalKontaktinformasjon";

    @Inject
    public DigitalKontaktinfoConsumer(@Value("${dkif_url}") String dkifUrl,
                                      RestTemplateBuilder restTemplateBuilder,
                                      StsRestConsumer stsRestConsumer) {
        this.dkifUrl = dkifUrl;
        this.stsRestConsumer = stsRestConsumer;
        this.restTemplate = restTemplateBuilder
                .setReadTimeout(Duration.ofSeconds(20))
                .setConnectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Retryable(include = DigitalKontaktinformasjonTechnicalException.class, maxAttempts = 5, backoff = @Backoff(delay = 200))
    @Metrics(value = DOK_CONSUMER, extraTags = {PROCESS_NAME, HENT_DIGITAL_KONTAKTINFORMASJON}, percentiles = {0.5, 0.95}, histogram = true)
    public DigitalKontaktinformasjonTo.DigitalKontaktinfo hentDigitalKontaktinfo(final String personidentifikator) {
        HttpHeaders headers = createHeaders();
        String fnrTrimmed = personidentifikator.trim();
        headers.add(NAV_PERSONIDENTER, fnrTrimmed);

        try {
            DigitalKontaktinformasjonTo response = restTemplate.exchange(dkifUrl + "/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false",
                    HttpMethod.GET, new HttpEntity<>(headers), DigitalKontaktinformasjonTo.class).getBody();
            if (isDigitalkontaktinformasjonValid(response, fnrTrimmed)) {
                return response.getKontaktinfo().get(fnrTrimmed);
            } else {
                return null;
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
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + stsRestConsumer.getOidcToken());
        headers.add(NAV_CONSUMER_ID, APP_NAME);
        headers.add(NAV_CALL_ID, MDC.get(MDC_CALL_ID));
        return headers;
    }
}