package no.nav.doknotifikasjon.repository;

import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotifikasjonDistribusjonRepository extends JpaRepository<NotifikasjonDistribusjon, Integer> {
}
