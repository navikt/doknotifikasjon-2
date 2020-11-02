package no.nav.doknotifikasjon.knot003.mapper;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
import no.nav.doknotifikasjon.exception.technical.AbstractDoknotifikasjonTechnicalException;
import no.nav.doknotifikasjon.exception.technical.DoknotifikasjonDBTechnicalException;
import no.nav.doknotifikasjon.knot003.domain.DoknotifikasjonEpost;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_SHORT;
import static no.nav.doknotifikasjon.constants.RetryConstants.MULTIPLIER_SHORT;

@Slf4j
@Component
public class NotifikasjonEntityMapper {
	private final int MAX_ATTEMPTS = 3;

	private final NotifikasjonDistribusjonRepository repository;

	public NotifikasjonEntityMapper(NotifikasjonDistribusjonRepository repository) {
		this.repository = repository;
	}

	@Retryable(include = AbstractDoknotifikasjonTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public DoknotifikasjonEpost mapNotifikasjonDistrubisjon(int notifikasjonDistribusjonId) throws Exception {
		try {
			NotifikasjonDistribusjon notifikasjonDistribusjonEntity = repository.findById(notifikasjonDistribusjonId).orElseThrow();
			Notifikasjon notifikasjonEntity = notifikasjonDistribusjonEntity.getNotifikasjon();

			return DoknotifikasjonEpost
					.builder()
					.notifikasjonDistribusjonId(String.valueOf(notifikasjonDistribusjonId))
					.bestillerId(notifikasjonEntity.getBestillerId())
					.bestillingsId(notifikasjonEntity.getBestillingsId())
					.distribusjonStatus(notifikasjonDistribusjonEntity.getStatus())
					.kanal(notifikasjonDistribusjonEntity.getKanal())
					.kontakt(notifikasjonDistribusjonEntity.getKontaktInfo())
					.tekst(notifikasjonDistribusjonEntity.getTekst())
					.build();
		} catch (DoknotifikasjonDistribusjonIkkeFunnetException exception) {
			log.error(
					"knot003 mapNotifikasjon fant ikke distribusjon notifikasjonDistribusjonId={}",
					notifikasjonDistribusjonId,
					exception
			);
			throw exception;
		} catch (TransientDataAccessException exception) {
			log.error(
					"knot003 mapNotifikasjon feilet midlertidig ved henting av distribusjon notifikasjonDistribusjonId={}",
					notifikasjonDistribusjonId,
					exception
			);
			throw new DoknotifikasjonDBTechnicalException(
					"knot003 mapNotifikasjon feilet midlertidig ved henting av distribusjon notifikasjonDistribusjonId=" + notifikasjonDistribusjonId,
					exception
			);
		} catch (DataAccessException exception) {
			log.error(
					"knot003 mapNotifikasjon feilet ved henting av distribusjon notifikasjonDistribusjonId={}",
					notifikasjonDistribusjonId,
					exception
			);
			throw new DoknotifikasjonDBTechnicalException(
					"knot003 mapNotifikasjon feilet ved henting av distribusjon notifikasjonDistribusjonId=" + notifikasjonDistribusjonId,
					exception
			);
		} catch (Exception exception) {
			log.error(
					"knot003 mapNotifikasjon feilet med ukjent feil notifikasjonDistribusjonId={}",
					notifikasjonDistribusjonId,
					exception
			);
			throw new DoknotifikasjonDBTechnicalException(
					"knot003 mapNotifikasjon feilet med ukjent feil notifikasjonDistribusjonId=" + notifikasjonDistribusjonId,
					exception
			);
		}
	}

	@Retryable(backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void updateEntity(int notifikasjonDistribusjonId, String bestillerId) {

		try {
			NotifikasjonDistribusjon notifikasjonDistribusjonEntity = repository.findById(notifikasjonDistribusjonId).orElseThrow();

			notifikasjonDistribusjonEntity.setEndretAv(bestillerId);
			notifikasjonDistribusjonEntity.setStatus(Status.FERDIGSTILT);

			LocalDateTime now = LocalDateTime.now();

			notifikasjonDistribusjonEntity.setSendtDato(now);
			notifikasjonDistribusjonEntity.setEndretDato(now);

		} catch (DoknotifikasjonDistribusjonIkkeFunnetException exception) {
			log.error(
					"knot003 updateEntity fant ikke distribusjon notifikasjonDistribusjonId={}",
					notifikasjonDistribusjonId,
					exception
			);
			throw exception;
		} catch (TransientDataAccessException exception) {
			log.error(
					"knot003 updateEntity feilet midlertidig ved henting av distribusjon notifikasjonDistribusjonId={} bestillerId={}",
					notifikasjonDistribusjonId,
					bestillerId,
					exception
			);
			throw new DoknotifikasjonDBTechnicalException(
					String.format(
							"knot003 updateEntity feilet midlertidig ved henting av distribusjon notifikasjonDistribusjonId=%d bestillerId=%s",
							notifikasjonDistribusjonId,
							bestillerId
					),
					exception
			);
		} catch (DataAccessException exception) {
			log.error(
					"knot003 updateEntity feilet ved henting av distribusjon notifikasjonDistribusjonId={} bestillerId={}",
					notifikasjonDistribusjonId,
					bestillerId,
					exception
			);
			throw new DoknotifikasjonDBTechnicalException(
					String.format(
							"knot003 updateEntity feilet ved henting av distribusjon notifikasjonDistribusjonId=%d bestillerId=%s",
							notifikasjonDistribusjonId,
							bestillerId
					),
					exception
			);
		} catch (Exception exception) {
			log.error(
					"knot003 updateEntity feilet med ukjent feil notifikasjonDistribusjonId={} bestillerId={}",
					notifikasjonDistribusjonId,
					bestillerId,
					exception
			);
			throw new DoknotifikasjonDBTechnicalException(
					String.format(
							"knot003 updateEntity feilet med ukjent feil notifikasjonDistribusjonId=%d bestillerId=%s",
							notifikasjonDistribusjonId,
							bestillerId
					),
					exception
			);
		}
	}

	@Retryable(maxAttempts = MAX_ATTEMPTS, backoff = @Backoff(delay = 5000))
	private NotifikasjonDistribusjon queryRepository(int notifikasjonDistribusjonId) {
		return repository.findById(notifikasjonDistribusjonId).orElseThrow(
				() -> {
					log.warn("NotifikasjonDistribusjon ikke funnet i databasen id={}", notifikasjonDistribusjonId);
					throw new DoknotifikasjonDistribusjonIkkeFunnetException(
							"NotifikasjonDistribusjon ikke funnet i databasen id=" + notifikasjonDistribusjonId
					);
				}
		);
	}
}
