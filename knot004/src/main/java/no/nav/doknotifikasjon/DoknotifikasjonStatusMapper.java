package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.springframework.stereotype.Component;

@Component
public class DoknotifikasjonStatusMapper {

    public DoknotifikasjonStatusTo map(DoknotifikasjonStatus doknotifikasjonStatus) {
        return DoknotifikasjonStatusTo.builder()
                .bestillerId(doknotifikasjonStatus.getBestillerId())
                .bestillingId(doknotifikasjonStatus.getBestillingsId())
                .status(doknotifikasjonStatus.getStatus())
                .melding(doknotifikasjonStatus.getMelding())
                .distribusjonId(doknotifikasjonStatus.getDistribusjonId())
                .build();
    }
}
