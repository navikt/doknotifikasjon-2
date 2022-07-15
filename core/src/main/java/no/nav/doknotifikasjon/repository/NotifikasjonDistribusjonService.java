package no.nav.doknotifikasjon.repository;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
import no.nav.doknotifikasjon.exception.technical.DoknotifikasjonDBTechnicalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static no.nav.doknotifikasjon.constants.RetryConstants.DATABASE_RETRIES;
import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_DATABASE_IKKE_OPPDATERT;

@Slf4j
@Component
public class NotifikasjonDistribusjonService {

	private final NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@Autowired
	NotifikasjonDistribusjonService(NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository) {
		this.notifikasjonDistribusjonRepository = notifikasjonDistribusjonRepository;
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxAttempts = DATABASE_RETRIES, backoff = @Backoff(delay = DELAY_LONG), exclude = DataIntegrityViolationException.class)
	public NotifikasjonDistribusjon save(NotifikasjonDistribusjon notifikasjonDistribusjon) {
		try {
			return notifikasjonDistribusjonRepository.save(notifikasjonDistribusjon);
		} catch (Exception e) {
			log.warn(FEILET_DATABASE_IKKE_OPPDATERT, e);
			throw new DoknotifikasjonDBTechnicalException(e.getMessage(), e);
		}
	}

	@Retryable(maxAttemptsExpression = "${retry.attempts:200}", backoff = @Backoff(delayExpression = "${retry.delay:50}"))
	public NotifikasjonDistribusjon findById(int notifikasjonDistribusjonId) {
		return notifikasjonDistribusjonRepository.findById(notifikasjonDistribusjonId).orElseThrow(
				() -> {
					log.info(String.format("NotifikasjonDistribusjon med id=%s ble ikke funnet i databasen.", notifikasjonDistribusjonId));
					throw new DoknotifikasjonDistribusjonIkkeFunnetException(String.format(
							"NotifikasjonDistribusjon med id=%s ble ikke funnet i databasen.", notifikasjonDistribusjonId)
					);
				});
	}

	@Retryable(maxAttempts = DATABASE_RETRIES, backoff = @Backoff(delay = DELAY_LONG))
	public Optional<NotifikasjonDistribusjon> findFirstByNotifikasjonInAndKanal(Notifikasjon notifikasjon, Kanal kanal) {
		return notifikasjonDistribusjonRepository.findFirstByNotifikasjonAndKanal(notifikasjon, kanal);
	}

	@Retryable(maxAttempts = DATABASE_RETRIES, backoff = @Backoff(delay = DELAY_LONG))
	public Optional<NotifikasjonDistribusjon> findFirstByNotifikasjonAndKanalAndEndretDatoIsNotNullOrderByEndretDatoDesc(Notifikasjon notifikasjon, Kanal kanal) {
		return notifikasjonDistribusjonRepository.findFirstByNotifikasjonAndKanalAndEndretDatoIsNotNullOrderByEndretDatoDesc(notifikasjon, kanal);
	}
}
