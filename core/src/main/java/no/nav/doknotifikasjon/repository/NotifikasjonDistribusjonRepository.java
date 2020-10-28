package no.nav.doknotifikasjon.repository;

import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotifikasjonDistribusjonRepository extends JpaRepository<NotifikasjonDistribusjon, Integer> {

    List<NotifikasjonDistribusjon> findAllByNotifikasjonIn(List<Notifikasjon> notifikasjonList);

    List<NotifikasjonDistribusjon> findAllByNotifikasjonAndStatus(Notifikasjon notifikasjon, Status status);
}
