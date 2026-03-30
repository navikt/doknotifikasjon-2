package no.nav.doknotifikasjon.consumer.altinn;

import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.Optional;
import java.util.UUID;

import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;

public interface AltinnVarselConsumer {

	Optional<UUID> sendVarsel(Kanal kanal, String bestillingsId, String kontaktInfo, String fnr, String tekst, String tittel);

	@Retryable(
			retryFor = AltinnTechnicalException.class,
			maxAttemptsExpression = "${retry.attempts:10}",
			backoff = @Backoff(delayExpression = "${retry.delay:1000}", multiplier = 2, maxDelay = 60_000L)
	)
	default Optional<UUID> sendEpostVarsel(String bestillingsId, String kontaktInfo, String fnr, String tekst, String tittel) {
		return sendVarsel(EPOST, bestillingsId, kontaktInfo, fnr, tekst, tittel);
	}

	@Retryable(
			retryFor = AltinnTechnicalException.class,
			maxAttemptsExpression = "${retry.attempts:10}",
			backoff = @Backoff(delayExpression = "${retry.delay:1000}", multiplier = 2, maxDelay = 60_000L)
	)
	default Optional<UUID> sendSmsVarsel(String bestillingsId, String kontaktInfo, String fnr, String tekst) {
		return sendVarsel(SMS, bestillingsId, kontaktInfo, fnr, tekst, "");
	}
}
