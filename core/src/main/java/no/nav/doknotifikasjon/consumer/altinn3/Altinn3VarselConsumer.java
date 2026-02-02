package no.nav.doknotifikasjon.consumer.altinn3;

import lombok.extern.slf4j.Slf4j;
import no.altinn.services.altinn3.openapi.domain.EmailSendingOptionsExt;
import no.altinn.services.altinn3.openapi.domain.NotificationOrderChainRequestExt;
import no.altinn.services.altinn3.openapi.domain.NotificationOrderChainResponseExt;
import no.altinn.services.altinn3.openapi.domain.NotificationRecipientExt;
import no.altinn.services.altinn3.openapi.domain.RecipientEmailExt;
import no.altinn.services.altinn3.openapi.domain.RecipientSmsExt;
import no.altinn.services.altinn3.openapi.domain.SmsSendingOptionsExt;
import no.altinn.services.altinn3.openapi.domain.ValidationProblemDetails;
import no.nav.doknotifikasjon.config.properties.Altinn3Props;
import no.nav.doknotifikasjon.consumer.altinn.AltinnVarselConsumer;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.metrics.Metrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.handler.UserRoleAuthorizationInterceptor;

import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_ALTIN_CONSUMER;

@Slf4j
@Profile("altinn3")
@Service
public class Altinn3VarselConsumer implements AltinnVarselConsumer {
	private static final String NAV_SMS_AVSENDER_DISPLAY_NAME = "Nav";
	private static final String IKKE_BESVAR_DENNE_NAV = "ikke-besvar-denne@nav.no";

	private final RestClient restClient;

	public Altinn3VarselConsumer(@Qualifier("naisTexasMaskinportenAuthenticatedRestClient") RestClient naisTexasMaskinportenAuthenticatedRestClient, Altinn3Props altinn3Props) {
		this.restClient = naisTexasMaskinportenAuthenticatedRestClient.mutate()
			.baseUrl(altinn3Props.notificationOrderUri())
			.build();
	}

	@Metrics(value = DOK_ALTIN_CONSUMER, createErrorMetric = true, errorMetricInclude = AltinnTechnicalException.class)
	@Retryable(
		retryFor = AltinnTechnicalException.class,
		maxAttemptsExpression = "${retry.attempts:10}",
		backoff = @Backoff(delayExpression = "${retry.delay:1000}", multiplier = 2, maxDelay = 60_000L)
	)
	public Optional<UUID> sendVarsel(Kanal kanal, String bestillingsId, String kontaktInfo, String fnr, String tekst, String tittel) {
		try {
			var entity = restClient.post().body(
					switch (kanal) {
						case SMS -> createSmsOrder(bestillingsId, kontaktInfo, tekst);
						case EPOST -> createEmailOrder(bestillingsId, kontaktInfo, tekst, tittel);
					})
				.retrieve()
				.toEntity(NotificationOrderChainResponseExt.class);

			if (entity.getStatusCode() == HttpStatus.OK) {
				log.info("Kall mot Altinn gikk OK men altinn mener de allerede har behandlet bestilling med denne ID-en. BestillingsId={}", bestillingsId);
			}
			return Optional.ofNullable(entity.getBody()).map(NotificationOrderChainResponseExt::getNotificationOrderId);
		} catch (RestClientResponseException e) {
			if (e.getStatusCode() == HttpStatus.BAD_REQUEST || e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
				ValidationProblemDetails validationProblemDetails = e.getResponseBodyAs(ValidationProblemDetails.class);
				throw new AltinnFunctionalException(format("Funksjonell feil i kall mot Altinn. %s, errorTitle=%s, errorMessage=%s", e.getStatusCode(), validationProblemDetails.getTitle(), validationProblemDetails.getDetail()), e);
			}
			throw new AltinnTechnicalException(format("Teknisk feil i kall mot Altinn. %s", e.getStatusCode()), e);
		} catch (Exception e) {
			throw new AltinnTechnicalException("Ukjent teknisk feil ved kall mot Altinn.", e);
		}
	}

	private static NotificationOrderChainRequestExt createEmailOrder(String bestillingsId, String kontaktInfo, String tekst, String tittel) {
		return NotificationOrderChainRequestExt.builder()
			.sendersReference(bestillingsId)
			.idempotencyId(bestillingsId)
			.recipient(NotificationRecipientExt.builder()
				.recipientEmail(RecipientEmailExt.builder().emailAddress(kontaktInfo).emailSettings(EmailSendingOptionsExt.builder()
						.subject(tittel)
						.body(tekst)
						.senderEmailAddress(IKKE_BESVAR_DENNE_NAV)
						.build())
					.build())
				.build())
			.build();
	}

	private static NotificationOrderChainRequestExt createSmsOrder(String bestillingsId, String kontaktInfo, String tekst) {
		return NotificationOrderChainRequestExt.builder()
			.sendersReference(bestillingsId)
			.idempotencyId(bestillingsId)
			.recipient(NotificationRecipientExt.builder()
				.recipientSms(RecipientSmsExt.builder()
					.phoneNumber(kontaktInfo)
					.smsSettings(SmsSendingOptionsExt.builder()
						.body(tekst)
						.sender(NAV_SMS_AVSENDER_DISPLAY_NAME)
						.build())
					.build())
				.build())
			.build();
	}

	@Recover
	public Optional<UUID> altinnTechnicalExceptionRecovery(AltinnTechnicalException e) {
		log.warn("Teknisk feil for sending av sms/epost til Altinn - maks. antall forsøk brukt");

		throw e;
	}

	// Catch-all for alle andre exceptions - hvis ikke blir ExhaustedRetryException kastet med meldingen 'Cannot locate recovery method'
	@Recover
	public Optional<UUID> otherExceptionsRecovery(RuntimeException e) {
		throw e;
	}

}
