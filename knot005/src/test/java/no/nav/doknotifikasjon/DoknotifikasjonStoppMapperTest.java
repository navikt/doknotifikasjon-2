package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp;
import org.junit.jupiter.api.Test;

import static no.nav.doknotifikasjon.TestUtils.BESTILLER_ID;
import static no.nav.doknotifikasjon.TestUtils.BESTILLINGS_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DoknotifikasjonStoppMapperTest {

    private final DoknotifikasjonStoppMapper doknotifikasjonStoppMapper = new DoknotifikasjonStoppMapper();

    @Test
    void shouldMap() {
        DoknotifikasjonStopp doknotifikasjonStopp = new DoknotifikasjonStopp(BESTILLINGS_ID, BESTILLER_ID);
        DoknotifikasjonStoppTo doknotifikasjonStoppTo = doknotifikasjonStoppMapper.map(doknotifikasjonStopp);

        assertEquals(doknotifikasjonStopp.getBestillingsId(), doknotifikasjonStoppTo.getBestillingsId());
        assertEquals(doknotifikasjonStopp.getBestillerId(), doknotifikasjonStoppTo.getBestillerId());
    }
}