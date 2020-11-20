package no.nav.doknotifikasjon.repository;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.model.Notifikasjon;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import java.time.LocalDate;
import java.util.List;

import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;
import static no.nav.doknotifikasjon.constants.RetryConstants.MAX_INT;

@Slf4j
@Component
public class NotifikasjonService {

	private final NotifikasjonRepository notifikasjonRepository;

	@Inject
	NotifikasjonService(NotifikasjonRepository notifikasjonRepository) {
		this.notifikasjonRepository = notifikasjonRepository;
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxAttempts = MAX_INT, backoff = @Backoff(delay = DELAY_LONG))
	public Notifikasjon save(Notifikasjon notifikasjon) {
		return notifikasjonRepository.save(notifikasjon);
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxAttempts = MAX_INT, backoff = @Backoff(delay = DELAY_LONG))
	public boolean existsByBestillingsId(String notifikasjon) {
		return notifikasjonRepository.existsByBestillingsId(notifikasjon);
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxAttempts = MAX_INT, backoff = @Backoff(delay = DELAY_LONG))
	public List<Notifikasjon> findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoIsLessThanEqual(Status status, Integer antallRenotifikasjoner, LocalDate nesteRenotifikasjonDato) {
		return notifikasjonRepository.findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoIsLessThanEqual(
				status,
				antallRenotifikasjoner,
				nesteRenotifikasjonDato
		);
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxAttempts = MAX_INT, backoff = @Backoff(delay = DELAY_LONG))
	public Notifikasjon findByBestillingsId(String bestillingsId) {
		return notifikasjonRepository.findByBestillingsId(bestillingsId);
	}
}

