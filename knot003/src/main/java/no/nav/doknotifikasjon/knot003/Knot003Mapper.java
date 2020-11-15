package no.nav.doknotifikasjon.knot003;

import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.springframework.stereotype.Component;

@Component
public class Knot003Mapper {

    public DoknotifikasjonEpostObject mapNotifikasjonDistrubisjon(NotifikasjonDistribusjon notifikasjonDistribusjon, Notifikasjon notifikasjon) {
        return DoknotifikasjonEpostObject
                .builder()
                .notifikasjonDistribusjonId(notifikasjonDistribusjon.getId())
                .bestillerId(notifikasjon.getBestillerId())
                .bestillingsId(notifikasjon.getBestillingsId())
                .distribusjonStatus(notifikasjonDistribusjon.getStatus())
                .kanal(notifikasjonDistribusjon.getKanal())
                .kontaktInfo(notifikasjonDistribusjon.getKontaktInfo())
                .tekst(notifikasjonDistribusjon.getTekst())
                .tittel(notifikasjonDistribusjon.getTittel())
                .fodselsnummer(notifikasjon.getMottakerId())
                .build();
    }
}
