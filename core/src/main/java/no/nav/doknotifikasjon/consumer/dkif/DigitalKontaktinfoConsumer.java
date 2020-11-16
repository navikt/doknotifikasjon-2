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
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_DKIF_CONSUMER;
import static no.nav.doknotifikasjon.metrics.MetricTags.HENT_DIGITAL_KONTAKTINFORMASJON;
import static no.nav.doknotifikasjon.metrics.MetricTags.PROCESS_NAME;

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

    @Retryable(include = DigitalKontaktinformasjonTechnicalException.class, maxAttempts = 5, backoff = @Backoff(delay = 200))
    @Metrics(value = DOK_DKIF_CONSUMER, extraTags = {PROCESS_NAME, HENT_DIGITAL_KONTAKTINFORMASJON})
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

    private String tmp = "eyJraWQiOiIzODVkOWYwZS1hNzRjLTQ0NGYtYjNmZi03NDRjYWVlNWEzZmYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJzcnZwZW5zam9uIiwiYXVkIjpbInNydnBlbnNqb24iLCJwcmVwcm9kLmxvY2FsIl0sInZlciI6IjEuMCIsIm5iZiI6MTYwNTUxNzY0NiwiYXpwIjoic3J2cGVuc2pvbiIsImlkZW50VHlwZSI6IlN5c3RlbXJlc3N1cnMiLCJhdXRoX3RpbWUiOjE2MDU1MTc2NDYsImlzcyI6Imh0dHBzOlwvXC9zZWN1cml0eS10b2tlbi1zZXJ2aWNlLm5haXMucHJlcHJvZC5sb2NhbCIsImV4cCI6MTYwNTUyMTI0NiwiaWF0IjoxNjA1NTE3NjQ2LCJqdGkiOiI2OTgzODk0ZC0yNTU4LTQ2ZjYtOTI5MS1lNTZkMWM1NTFmNmQifQ.B5hhv73q3LDaVe8iXnxEpfD-xpOrcqTTviqnqmtTENI8mRMOnMSNOa06bvF_OtCqZfSYSp5ClsI9ZWKimqG3IqiOMul0NT8dKEGXhITv0i0X7KrllZQyLQxgnBR8M1UJYz7KbcL6MrAMYiASogi9jr_4DXIWOSFi-SWK2wrlirXsPxghTKYDChfufEJMqAaJ6L-i1JBtGu4suMxOTP-IJ_zb_lCKs4jwGJGbQhDX0daL8BQx_uZk55Fwr3xX0cor1Q7uY4LQllpE7M2NQSv9x2iOhthk8MmJBnIWXQ-VX1B7I-o9OShsy7GbQc8Y0mpsfZBr4HgxVpSbAarb65_caw";

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + tmp);
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