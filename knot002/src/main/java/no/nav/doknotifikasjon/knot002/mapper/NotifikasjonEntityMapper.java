package no.nav.doknotifikasjon.knot002.mapper;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.knot002.domain.DoknotifikasjonSms;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Slf4j
@Component
public class NotifikasjonEntityMapper {
    private final int MAX_ATTEMPTS = 3;

    private final NotifikasjonDistribusjonRepository repository;
    public NotifikasjonEntityMapper(NotifikasjonDistribusjonRepository repository){
        this.repository = repository;
    }

    @Retryable(maxAttempts = MAX_ATTEMPTS, backoff = @Backoff(delay = 3000))
    public DoknotifikasjonSms mapNotifikasjon(int notifikasjonDistribusjonId) throws Exception {
        try {
            NotifikasjonDistribusjon notifikasjonDistribusjonEntity = repository.findById(notifikasjonDistribusjonId).orElseThrow();
            Notifikasjon notifikasjonEntity = notifikasjonDistribusjonEntity.getNotifikasjon();

            return DoknotifikasjonSms
                    .builder()
                    .notifikasjonDistribusjonId(String.valueOf(notifikasjonDistribusjonId))
                    .bestillerId(notifikasjonEntity.getBestillerId())
                    .bestillingsId(notifikasjonEntity.getBestillingsId())
                    .distribusjonStatus(notifikasjonDistribusjonEntity.getStatus())
                    .kanal(notifikasjonDistribusjonEntity.getKanal())
                    .kontakt(notifikasjonDistribusjonEntity.getKontaktInfo())
                    .tekst(notifikasjonDistribusjonEntity.getTekst())
                    .build();
        } catch (NoSuchElementException exception){
            log.warn(
                    "knot002 mapNotifikasjon fant ikke distribusjon notifikasjonDistribusjonId=${}",
                    notifikasjonDistribusjonId,
                    exception
            );
            throw exception;
        } catch (TransientDataAccessException exception){
            log.warn(
                    "knot002 mapNotifikasjon feilet midlertidig ved henting av distribusjon notifikasjonDistribusjonId=${}",
                    notifikasjonDistribusjonId,
                    exception
            );
            throw exception;
        } catch(DataAccessException exception){
            log.warn(
                    "knot002 mapNotifikasjon feilet ved henting av distribusjon notifikasjonDistribusjonId=${}",
                    notifikasjonDistribusjonId,
                    exception
            );
            throw exception;
        } catch (Exception exception) {
            log.warn(
                    "knot002 mapNotifikasjon feilet med ukjent feil notifikasjonDistribusjonId=${}",
                    notifikasjonDistribusjonId,
                    exception
            );
            throw exception;
        }
    }

    @Recover
    public DoknotifikasjonSms recoverMapNotifikasjon(Exception e, int notifikasjonDistribusjonId) throws Exception {
        log.error(
                "knot002 mapNotifikasjon retry mislykkes, antall forsøk=${}, DistribusjonId=${}",
                MAX_ATTEMPTS,
                notifikasjonDistribusjonId,
                e
        );
        throw e;
    }

    @Retryable(maxAttempts = MAX_ATTEMPTS, backoff = @Backoff(delay = 3000))
    public void updateEntity(int notifikasjonDistribusjonId, String bestillerId) {

        try {
            NotifikasjonDistribusjon notifikasjonDistribusjonEntity = repository.findById(notifikasjonDistribusjonId).orElseThrow();

            notifikasjonDistribusjonEntity.setEndretAv(bestillerId);
            notifikasjonDistribusjonEntity.setStatus(Status.FERDIGSTILT);

            LocalDateTime now = LocalDateTime.now();

            notifikasjonDistribusjonEntity.setSendtDato(now);
            notifikasjonDistribusjonEntity.setEndretDato(now);

        } catch (TransientDataAccessException exception){
            log.warn(
                    "knot002 updateEntity feilet midlertidig ved henting av distribusjon notifikasjonDistribusjonId=${} bestillerId=${}",
                    notifikasjonDistribusjonId,
                    bestillerId,
                    exception
            );
            throw exception;
        } catch(DataAccessException exception){
            log.warn(
                    "knot002 updateEntity feilet ved henting av distribusjon notifikasjonDistribusjonId=${} bestillerId=${}",
                    notifikasjonDistribusjonId,
                    bestillerId,
                    exception
            );
            throw exception;
        } catch (Exception exception) {
            log.warn(
                    "knot002 updateEntity feilet med ukjent feil notifikasjonDistribusjonId=${} bestillerId=${}",
                    notifikasjonDistribusjonId,
                    bestillerId,
                    exception
            );
            throw exception;
        }
    }
}
