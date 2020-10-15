package no.nav.doknotifikasjon.repository;

import no.nav.doknotifikasjon.model.Notifikasjon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotifikasjonRepository extends JpaRepository<Notifikasjon, Integer> {
    public Optional<Notifikasjon> findFirstByBestillerId(String bestillerId);
}
