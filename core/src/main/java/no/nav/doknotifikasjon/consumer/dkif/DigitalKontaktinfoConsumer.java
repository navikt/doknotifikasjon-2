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
import java.util.UUID;

import static java.lang.String.format;
import static no.nav.doknotifikasjon.constants.DomainConstants.APP_NAME;
import static no.nav.doknotifikasjon.constants.DomainConstants.BEARER_PREFIX;
import static no.nav.doknotifikasjon.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doknotifikasjon.constants.MDCConstants.NAV_CALL_ID;
import static no.nav.doknotifikasjon.constants.MDCConstants.NAV_CONSUMER_ID;
import static no.nav.doknotifikasjon.constants.MDCConstants.NAV_PERSONIDENTER;
import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;
import static no.nav.doknotifikasjon.constants.RetryConstants.MAX_INT;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_DKIF_CONSUMER;

@Slf4j
@Component
public class DigitalKontaktinfoConsumer implements DigitalKontaktinformasjon {

    private final String dkifUrl;
    private final RestTemplate restTemplate;
    private final StsRestConsumer stsRestConsumer;

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

    @Metrics(value = DOK_DKIF_CONSUMER, createErrorMetric = true, errorMetricInclude = DigitalKontaktinformasjonTechnicalException.class)
    @Retryable(include = DigitalKontaktinformasjonTechnicalException.class, maxAttempts = MAX_INT, backoff = @Backoff(delay = DELAY_LONG))
    public DigitalKontaktinformasjonTo hentDigitalKontaktinfo(final String personidentifikator) {
        HttpHeaders headers = createHeaders();
        String fnrTrimmed = personidentifikator.trim();
        headers.add(NAV_PERSONIDENTER, fnrTrimmed);

        try {
            return restTemplate.exchange(dkifUrl + "/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false", HttpMethod.GET, new HttpEntity<>(headers), DigitalKontaktinformasjonTo.class).getBody();
        } catch (HttpClientErrorException e) {
            throw new DigitalKontaktinformasjonFunctionalException(format("Funksjonell feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s", e
                    .getMessage()), e);
        } catch (HttpServerErrorException e) {
            throw new DigitalKontaktinformasjonTechnicalException(format("Teknisk feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s", e
                    .getMessage()), e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + stsRestConsumer.getOidcToken());
        headers.add(NAV_CONSUMER_ID, APP_NAME);
        headers.add(NAV_CALL_ID, this.getDefaultUuidIfNoCallIdIsSett());
        return headers;
    }

    private String getDefaultUuidIfNoCallIdIsSett() {
        if (MDC.get(MDC_CALL_ID) != null && !MDC.get(MDC_CALL_ID).trim().isEmpty()) {
            return MDC.get(MDC_CALL_ID);
        }
        return UUID.randomUUID().toString();
    }
}