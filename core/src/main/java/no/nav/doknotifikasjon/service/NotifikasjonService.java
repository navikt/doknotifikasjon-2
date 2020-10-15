package no.nav.doknotifikasjon.service;


import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotifikasjonService {

    @Autowired
    NotifikasjonRepository repo;

    public boolean notifikasjonExistByBestillingsId(String bestilingsId) {
        return repo.findFirstByBestillerId(bestilingsId).isPresent();
    }

    public Notifikasjon save (Notifikasjon notifikasjon) {
        return repo.save(notifikasjon);
    }

}
