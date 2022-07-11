package no.nav.doknotifikasjon.repository;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.NotifikasjonIkkeFunnetException;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.model.Notifikasjon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

import static no.nav.doknotifikasjon.constants.RetryConstants.DATABASE_RETRIES;
import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;

@Slf4j
@Component
public class NotifikasjonService {

	private final NotifikasjonRepository notifikasjonRepository;

	@Autowired
	NotifikasjonService(NotifikasjonRepository notifikasjonRepository) {
		this.notifikasjonRepository = notifikasjonRepository;
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxAttempts = DATABASE_RETRIES, backoff = @Backoff(delay = DELAY_LONG), exclude = DataIntegrityViolationException.class)
	public Notifikasjon save(Notifikasjon notifikasjon) {
		return notifikasjonRepository.save(notifikasjon);
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxAttempts = DATABASE_RETRIES, backoff = @Backoff(delay = DELAY_LONG))
	public boolean existsByBestillingsId(String notifikasjon) {
		return notifikasjonRepository.existsByBestillingsId(notifikasjon);
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxAttempts = DATABASE_RETRIES, backoff = @Backoff(delay = DELAY_LONG))
	public List<Notifikasjon> findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoIsLessThanEqual(Status status, Integer antallRenotifikasjoner, LocalDate nesteRenotifikasjonDato) {
		return notifikasjonRepository.findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoIsLessThanEqual(
				status,
				antallRenotifikasjoner,
				nesteRenotifikasjonDato
		);
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxAttempts = DATABASE_RETRIES, backoff = @Backoff(delay = DELAY_LONG))
	public List<Notifikasjon> findAllWithStatusOpprettetOrOversendtAndNoRenotifikasjoner() {
		return notifikasjonRepository.findAllWithStatusOpprettetOrOversendtAndNoRenotifikasjoner();
	}

	@Metrics(createErrorMetric = true, errorMetricExclude = NotifikasjonIkkeFunnetException.class)
	@Retryable(value = NotifikasjonIkkeFunnetException.class, recover = "notifikasjonIkkeFunnetRecovery", backoff = @Backoff(delayExpression = "${retry.delay:1000}"))
	public Notifikasjon findByBestillingsId(String bestillingsId) {
		return notifikasjonRepository.findByBestillingsId(bestillingsId).orElseThrow(
				() -> {
					throw new NotifikasjonIkkeFunnetException(String.format(
							"Notifikasjon med bestillingsId=%s ble ikke funnet i databasen.", bestillingsId)
					);
				});
	}

	@Recover
	public Notifikasjon notifikasjonIkkeFunnetRecovery(NotifikasjonIkkeFunnetException e, String bestillingsId) {
		log.info("Notifikasjon med bestillingsId={} ble ikke funnet i databasen etter {} forsøk.", bestillingsId, 3);
		return null;
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxAttemptsExpression = "${retry.attempts:200}", backoff = @Backoff(delayExpression = "${retry.delay:1000}"))
	public Notifikasjon findByBestillingsIdIngenRetryForNotifikasjonIkkeFunnet(String bestillingsId) {
		return notifikasjonRepository.findByBestillingsId(bestillingsId).orElse(null);
	}
}

