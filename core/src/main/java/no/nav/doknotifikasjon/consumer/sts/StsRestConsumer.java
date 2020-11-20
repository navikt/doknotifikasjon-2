package no.nav.doknotifikasjon.consumer.sts;


import no.nav.doknotifikasjon.config.ServiceuserAlias;
import no.nav.doknotifikasjon.exception.technical.StsTechnicalException;
import no.nav.doknotifikasjon.metrics.Metrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.Duration;

import static no.nav.doknotifikasjon.config.LokalCacheConfig.STS_CACHE;
import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;
import static no.nav.doknotifikasjon.constants.RetryConstants.MAX_INT;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_STS_CONSUMER;


@Component
public class StsRestConsumer {

	private final RestTemplate restTemplate;
	private final String stsUrl;

	@Inject
	public StsRestConsumer(@Value("${security-token-service-token.url}") String stsUrl, RestTemplateBuilder restTemplateBuilder,
						   final ServiceuserAlias serviceuserAlias) {
		this.stsUrl = stsUrl;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
	}

	@Cacheable(STS_CACHE)
	@Metrics(value = DOK_STS_CONSUMER, createErrorMetric = true, errorMetricInclude = StsTechnicalException.class)
	@Retryable(include = StsTechnicalException.class, maxAttempts = MAX_INT, backoff = @Backoff(delay = DELAY_LONG))
	public String getOidcToken() {
		try {
			StsResponseTo stsResponseTo = restTemplate.getForObject(stsUrl + "?grant_type=client_credentials&scope=openid", StsResponseTo.class);
			return stsResponseTo != null ? stsResponseTo.getAccessToken() : null;
		} catch (HttpStatusCodeException e) {
			throw new StsTechnicalException(String.format("Kall mot STS feilet med status=%s feilmelding=%s.", e.getStatusCode(), e
					.getMessage()), e);
		} catch (ResourceAccessException e) {
			throw new StsTechnicalException(String.format("Kall mot STS feilet med manglende tilgang: %s", e.getMessage()), e);
		}
	}
}
