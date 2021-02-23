package no.nav.doknotifikasjon.repository;

import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotifikasjonRepository extends JpaRepository<Notifikasjon, Integer> {
	Notifikasjon findByBestillingsId(String bestillingsId);

	boolean existsByBestillingsId(String bestillingsId);

	List<Notifikasjon> findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoIsLessThanEqual(Status status, Integer antallRenotifikasjoner, LocalDate nesteRenotifikasjonDato);

	@Query(
			value = "Select * from t_notifikasjon\n" +
					"where k_status = :status\n" +
					"  and (antall_renotifikasjoner == 0 or antall_renotifikasjoner is null)\n" +
					"  and endret_dato is not null\n" +
					"  and endret_dato >= :endretDato",
			nativeQuery=true
	)
	List<Notifikasjon> findAllByStatusAndEndretDatoIsGreaterThanEqualWithNoAntallRenotifikasjoner(
			@Param("status") String status,
			@Param("endretDato") LocalDateTime endretDato
	);
}
