package no.nav.doknotifikasjon.knot002.mapper;

import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.knot002.domain.DoknotifikasjonSms;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
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
    private final JpaRepository<NotifikasjonDistribusjon, Integer> repository;
    NotifikasjonEntityMapper(JpaRepository<NotifikasjonDistribusjon, Integer> repository){
        this.repository = repository;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 3000))
    public Either<Throwable, DoknotifikasjonSms> mapNotifikasjon(String notifikasjonDistribusjonId){
        try{
            Integer id = Integer.valueOf(notifikasjonDistribusjonId, 10);
            NotifikasjonDistribusjon notifikasjonDistribusjonEntity = repository.findById(id).orElseThrow();
            Notifikasjon notifikasjonEntity = notifikasjonDistribusjonEntity.getNotifikasjonId();

            return Either.right(DoknotifikasjonSms
                    .builder()
                    .notifikasjonDistribusjonId(notifikasjonDistribusjonId)
                    .bestillerId(notifikasjonEntity.getBestillerId())
                    .bestillingId(notifikasjonEntity.getBestillingId())
                    .distribusjonStatus(notifikasjonDistribusjonEntity.getStatus())
                    .kanal(notifikasjonDistribusjonEntity.getKanal())
                    .kontakt(notifikasjonDistribusjonEntity.getKontaktInfo())
                    .tekst(notifikasjonDistribusjonEntity.getTekst())
                    .build());
        } catch(Exception e){
            log.error("foo");
            throw e;
        }
    }

    @Recover
    public Either<Throwable, DoknotifikasjonSms> recoverForMapNotifikasjon(Exception e, String notifikasjonDistribusjonId){
        return Either.left(e);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 3000))
    public Optional<Throwable> updateEntity(String notifikasjonDistribusjonId, String bestillerId) {

        try {
            Integer id = Integer.valueOf(notifikasjonDistribusjonId, 10);

            NotifikasjonDistribusjon notifikasjonDistribusjonEntity = repository.findById(id).orElseThrow();

            notifikasjonDistribusjonEntity.setEndretAv(bestillerId);
            notifikasjonDistribusjonEntity.setStatus(Status.FERDIGSTILT);

            LocalDateTime now = LocalDateTime.now();

            notifikasjonDistribusjonEntity.setSendtDato(now);
            notifikasjonDistribusjonEntity.setEndretDato(now);

            repository.save(notifikasjonDistribusjonEntity);

            return Optional.empty();

        } catch(Exception e){
            log.error("foo");
            throw e;
        }
    }

    @Recover
    public Optional<Throwable> recoverForUpdateEntity(Exception e, String notifikasjonDistribusjonId, String bestillerId){
        log.error("feilet update entity", e);
        return Optional.of(e);
    }

}
