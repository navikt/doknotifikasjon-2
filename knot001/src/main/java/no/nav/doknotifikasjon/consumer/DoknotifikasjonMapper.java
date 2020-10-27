package no.nav.doknotifikasjon.consumer;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.schemas.PrefererteKanal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DoknotifikasjonMapper {

    public DoknotifikasjonTO map(Doknotifikasjon doknotifikasjon) {
        return DoknotifikasjonTO.builder()
                .bestillerId(doknotifikasjon.getBestillerId())
                .bestillingsId(doknotifikasjon.getBestillingsId())
                .fodselsnummer(doknotifikasjon.getFodselsnummer())
                .antallRenotifikasjoner(doknotifikasjon.getAntallRenotifikasjoner())
                .renotifikasjonIntervall(doknotifikasjon.getRenotifikasjonIntervall())
                .tittel(doknotifikasjon.getTittel())
                .epostTekst(doknotifikasjon.getEpostTekst())
                .smsTekst(doknotifikasjon.getSmsTekst())
                .prefererteKanaler(this.setDefaultPrefererteKanaler(doknotifikasjon.getPrefererteKanaler()))
                .build();
    }

    private List<Kanal> setDefaultPrefererteKanaler(List<PrefererteKanal> preferteKanaler) {
        List<Kanal> preferteKanalerTO = new ArrayList<>();

        if (preferteKanaler != null && !preferteKanaler.isEmpty()) {
            preferteKanaler.forEach(s -> {
                if (s.equals(PrefererteKanal.EPOST)) {
                    preferteKanalerTO.add(Kanal.EPOST);
                } else {
                    preferteKanalerTO.add(Kanal.SMS);
                }
            });
        } else {
            preferteKanalerTO.add(Kanal.EPOST);
        }

        return preferteKanalerTO;
    }
}
