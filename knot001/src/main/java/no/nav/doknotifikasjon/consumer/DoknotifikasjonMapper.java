package no.nav.doknotifikasjon.consumer;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.schemas.PrefererteKanal;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

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
				.sikkerhetsnivaa(doknotifikasjon.getSikkerhetsnivaa())
				.build();
	}

	private List<Kanal> setDefaultPrefererteKanaler(List<PrefererteKanal> preferteKanaler) {
		if (preferteKanaler == null || preferteKanaler.isEmpty()) {
			return singletonList(Kanal.EPOST);
		}

		return preferteKanaler.stream()
				.map(kanal -> kanal.equals(PrefererteKanal.EPOST) ? Kanal.EPOST : Kanal.SMS)
				.collect(Collectors.toList());
	}
}
