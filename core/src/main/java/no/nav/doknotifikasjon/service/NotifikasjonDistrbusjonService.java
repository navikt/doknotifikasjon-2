package no.nav.doknotifikasjon.service;


import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotifikasjonDistrbusjonService {

    @Autowired
    NotifikasjonDistrbusjonService repo;

    public NotifikasjonDistribusjon save (NotifikasjonDistribusjon notifikasjonDistribusjon) {
        return repo.save(notifikasjonDistribusjon);
    }

}
