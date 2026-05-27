package no.nav.doknotifikasjon.repository;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.NotifikasjonIkkeFunnetException;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.model.Notifikasjon;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

import static java.lang.String.format;
import static no.nav.doknotifikasjon.constants.RetryConstants.DATABASE_RETRIES;

@Slf4j
@Component
public class NotifikasjonService {

	private final NotifikasjonRepository notifikasjonRepository;

	NotifikasjonService(NotifikasjonRepository notifikasjonRepository) {
		this.notifikasjonRepository = notifikasjonRepository;
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxRetries = DATABASE_RETRIES, excludes = DataIntegrityViolationException.class)
	public Notifikasjon save(Notifikasjon notifikasjon) {
		return notifikasjonRepository.save(notifikasjon);
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxRetries = DATABASE_RETRIES)
	public boolean existsByBestillingsId(String notifikasjon) {
		return notifikasjonRepository.existsByBestillingsId(notifikasjon);
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxRetries = DATABASE_RETRIES)
	public List<Notifikasjon> findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoIsLessThanEqual(Status status, Integer antallRenotifikasjoner, LocalDate nesteRenotifikasjonDato) {
		return notifikasjonRepository.findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoIsLessThanEqual(
				status,
				antallRenotifikasjoner,
				nesteRenotifikasjonDato
		);
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxRetries = DATABASE_RETRIES)
	public List<Notifikasjon> findAllWithStatusOpprettetOrOversendtAndNoRenotifikasjoner() {
		return notifikasjonRepository.findAllWithStatusOpprettetOrOversendtAndNoRenotifikasjoner();
	}

	@Retryable(maxRetries = 4)
	public Notifikasjon findByBestillingsId(String bestillingsId) {
		return notifikasjonRepository.findByBestillingsId(bestillingsId).orElseThrow(() -> {
			log.info("Notifikasjon med bestillingsId={} ble ikke funnet i databasen.", bestillingsId);

			return new NotifikasjonIkkeFunnetException(format("Notifikasjon med bestillingsId=%s ble ikke funnet i databasen.", bestillingsId));
		});
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxRetries = DATABASE_RETRIES)
	public Notifikasjon findByBestillingsIdIngenRetryForNotifikasjonIkkeFunnet(String bestillingsId) {
		return notifikasjonRepository.findByBestillingsId(bestillingsId).orElse(null);
	}
}
