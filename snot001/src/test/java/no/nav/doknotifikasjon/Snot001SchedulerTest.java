package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Snot001SchedulerTest {

    @Autowired
    private NotifikasjonRepository notifikasjonRepository;

    @Autowired
    private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

    @BeforeEach
    public void setup() {
        notifikasjonDistribusjonRepository.deleteAll();
        notifikasjonRepository.deleteAll();
    }

    @Test
    void scheduledJob() {
    }


}