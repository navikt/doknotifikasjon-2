package no.nav.doknotifikasjon.repository;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
import no.nav.doknotifikasjon.exception.technical.DoknotifikasjonDBTechnicalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.metrics.Metrics;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.lang.String.format;
import static no.nav.doknotifikasjon.constants.RetryConstants.DATABASE_RETRIES;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_DATABASE_IKKE_OPPDATERT;

@Slf4j
@Component
public class NotifikasjonDistribusjonService {

	private final NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	NotifikasjonDistribusjonService(NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository) {
		this.notifikasjonDistribusjonRepository = notifikasjonDistribusjonRepository;
	}

	@Metrics(createErrorMetric = true)
	@Retryable(maxRetries = DATABASE_RETRIES, excludes = DataIntegrityViolationException.class)
	public NotifikasjonDistribusjon save(NotifikasjonDistribusjon notifikasjonDistribusjon) {
		try {
			return notifikasjonDistribusjonRepository.save(notifikasjonDistribusjon);
		} catch (Exception e) {
			log.warn(FEILET_DATABASE_IKKE_OPPDATERT, e);
			throw new DoknotifikasjonDBTechnicalException(e.getMessage(), e);
		}
	}

	@Retryable(maxRetries = 5, delay = 200, multiplier = 4, maxDelay = 60_000L)
	public NotifikasjonDistribusjon findById(int notifikasjonDistribusjonId) {
		return notifikasjonDistribusjonRepository.findById(notifikasjonDistribusjonId).orElseThrow(() -> {
			log.info("NotifikasjonDistribusjon med id={} ble ikke funnet i databasen.", notifikasjonDistribusjonId);

			return new DoknotifikasjonDistribusjonIkkeFunnetException(notifikasjonDistribusjonId, format(
					"NotifikasjonDistribusjon med id=%s ble ikke funnet i databasen.", notifikasjonDistribusjonId)
			);
		});
	}

	@Retryable(maxRetries = DATABASE_RETRIES)
	public Optional<NotifikasjonDistribusjon> findFirstByNotifikasjonInAndKanal(Notifikasjon notifikasjon, Kanal kanal) {
		return notifikasjonDistribusjonRepository.findFirstByNotifikasjonAndKanal(notifikasjon, kanal);
	}

	@Retryable(maxRetries = DATABASE_RETRIES)
	public Optional<NotifikasjonDistribusjon> findFirstByNotifikasjonAndKanalAndEndretDatoIsNotNullOrderByEndretDatoDesc(Notifikasjon notifikasjon, Kanal kanal) {
		return notifikasjonDistribusjonRepository.findFirstByNotifikasjonAndKanalAndEndretDatoIsNotNullOrderByEndretDatoDesc(notifikasjon, kanal);
	}
}
