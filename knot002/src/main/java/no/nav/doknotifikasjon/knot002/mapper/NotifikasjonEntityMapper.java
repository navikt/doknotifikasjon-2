package no.nav.doknotifikasjon.knot002.mapper;

import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.knot002.domain.DoknotifikasjonSms;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
public class NotifikasjonEntityMapper {
    private final NotifikasjonDistribusjonRepository repository;
    NotifikasjonEntityMapper(NotifikasjonDistribusjonRepository repository){
        this.repository = repository;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 3000))
    public DoknotifikasjonSms mapNotifikasjon(String notifikasjonDistribusjonId) {
        try {
            Integer id = Integer.valueOf(notifikasjonDistribusjonId);
            NotifikasjonDistribusjon notifikasjonDistribusjonEntity = repository.findById(id).orElseThrow();
            Notifikasjon notifikasjonEntity = notifikasjonDistribusjonEntity.getNotifikasjonId();

            return DoknotifikasjonSms
                    .builder()
                    .notifikasjonDistribusjonId(notifikasjonDistribusjonId)
                    .bestillerId(notifikasjonEntity.getBestillerId())
                    .bestillingsId(notifikasjonEntity.getBestillingId())
                    .distribusjonStatus(notifikasjonDistribusjonEntity.getStatus())
                    .kanal(notifikasjonDistribusjonEntity.getKanal())
                    .kontakt(notifikasjonDistribusjonEntity.getKontaktInfo())
                    .tekst(notifikasjonDistribusjonEntity.getTekst())
                    .build();
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
/*
    @Recover
    public DoknotifikasjonSms recoverForMapNotifikasjon(Exception exception, String notifikasjonDistribusjonId) throws Exception {
        log.warn(
                "knot002 mapNotifikasjon retry feilet 3 ganger notifikasjonDistribusjonId=${}",
                notifikasjonDistribusjonId,
                exception
        );
        throw exception;
    }

 */

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 3000))
    public void updateEntity(String notifikasjonDistribusjonId, String bestillerId) {

        try {
            Integer id = Integer.valueOf(notifikasjonDistribusjonId);

            NotifikasjonDistribusjon notifikasjonDistribusjonEntity = repository.findById(id).orElseThrow();

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
/*
    @Recover
    public void recoverForUpdateEntity(Exception exception, String notifikasjonDistribusjonId, String bestillerId){
        log.warn(
                "knot002 updateEntity retry feilet 3 ganger notifikasjonDistribusjonId=${} bestillerId=${}",
                notifikasjonDistribusjonId,
                bestillerId,
                exception
        );
    }

 */

}
