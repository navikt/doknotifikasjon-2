package no.nav.doknotifikasjon.consumer;

import lombok.Builder;
import lombok.Value;
import no.nav.doknotifikasjon.kodeverk.Kanal;

import java.util.List;

@Builder
@Value
public class DoknotifikasjonTO {
	String bestillingsId;
	String bestillerId;
	String fodselsnummer;
	Integer antallRenotifikasjoner;
	Integer renotifikasjonIntervall;
	String tittel;
	String epostTekst;
	String smsTekst;
	List<Kanal> prefererteKanaler;
}
