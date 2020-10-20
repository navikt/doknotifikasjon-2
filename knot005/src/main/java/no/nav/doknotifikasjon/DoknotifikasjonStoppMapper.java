package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp;
import org.springframework.stereotype.Component;

@Component
public class DoknotifikasjonStoppMapper {

    DoknotifikasjonStoppTo map(DoknotifikasjonStopp doknotifikasjonStopp) {
        return DoknotifikasjonStoppTo.builder()
                .bestillerId(doknotifikasjonStopp.getBestillerId())
                .bestillingsId(doknotifikasjonStopp.getBestillingsId())
                .build();
    }
}
