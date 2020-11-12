package no.nav.doknotifikasjon.knot002;

import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.springframework.stereotype.Component;

@Component
public class Knot002Mapper {

    public DoknotifikasjonSmsObject mapNotifikasjonDistrubisjon(NotifikasjonDistribusjon notifikasjonDistribusjon, Notifikasjon notifikasjon) {
        return DoknotifikasjonSmsObject.builder()
                .notifikasjonDistribusjonId(notifikasjonDistribusjon.getId())
                .bestillerId(notifikasjon.getBestillerId())
                .bestillingsId(notifikasjon.getBestillingsId())
                .distribusjonStatus(notifikasjonDistribusjon.getStatus())
                .kanal(notifikasjonDistribusjon.getKanal())
                .kontaktInfo(notifikasjonDistribusjon.getKontaktInfo())
                .tekst(notifikasjonDistribusjon.getTekst())
                .fodselsnummer(notifikasjon.getMottakerId())
                .build();
    }
}
