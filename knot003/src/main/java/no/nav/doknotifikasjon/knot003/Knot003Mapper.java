package no.nav.doknotifikasjon.knot003;

import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.springframework.stereotype.Component;

@Component
public class Knot003Mapper {

    public DoknotifikasjonEpostTo mapNotifikasjonDistrubisjon(NotifikasjonDistribusjon notifikasjonDistribusjon, Notifikasjon notifikasjon) {
        return DoknotifikasjonEpostTo
                .builder()
                .notifikasjonDistribusjonId(String.valueOf(notifikasjonDistribusjon.getId()))
                .bestillerId(notifikasjon.getBestillerId())
                .bestillingsId(notifikasjon.getBestillingsId())
                .distribusjonStatus(notifikasjonDistribusjon.getStatus())
                .kanal(notifikasjonDistribusjon.getKanal())
                .kontakt(notifikasjonDistribusjon.getKontaktInfo())
                .tekst(notifikasjonDistribusjon.getTekst())
                .tittel(notifikasjonDistribusjon.getTittel())
                .fodselsnummer(notifikasjon.getMottakerId())
                .build();
    }
}
