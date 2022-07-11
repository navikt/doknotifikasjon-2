package no.nav.doknotifikasjon.repository;

import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotifikasjonRepository extends JpaRepository<Notifikasjon, Integer> {

	Optional<Notifikasjon> findByBestillingsId(String bestillingsId);

	boolean existsByBestillingsId(String bestillingsId);

	List<Notifikasjon> findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoIsLessThanEqual(Status status, Integer antallRenotifikasjoner, LocalDate nesteRenotifikasjonDato);

	@Query(
			value = opprettetEllerOversendtQuery,
			nativeQuery = true
	)
	List<Notifikasjon> findAllWithStatusOpprettetOrOversendtAndNoRenotifikasjoner();

	String opprettetEllerOversendtQuery = """
			Select * from t_notifikasjon
			where k_status in ('OPPRETTET', 'OVERSENDT')
			  and (antall_renotifikasjoner = 0 or antall_renotifikasjoner is null)
			  and endret_dato is not null
			  and endret_dato >= current_date - 30
			  """;

}
