package no.nav.doknotifikasjon.repository;

import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NotifikasjonRepository extends JpaRepository<Notifikasjon, Integer> {
    Notifikasjon findByBestillingsId(String bestillingsId);

    List<Notifikasjon> findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoBefore(Status status, Integer antallRenotifikasjoner, LocalDate nesteRenotifikasjonDato);
}
