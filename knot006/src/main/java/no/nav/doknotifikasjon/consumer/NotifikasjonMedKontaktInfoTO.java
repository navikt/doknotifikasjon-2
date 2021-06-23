package no.nav.doknotifikasjon.consumer;

import lombok.Builder;
import lombok.Value;
import no.nav.doknotifikasjon.kodeverk.Kanal;

import java.util.List;

@Builder
@Value
public class NotifikasjonMedKontaktInfoTO {
	String bestillingsId;
	String bestillerId;
	String fodselsnummer;
	String mobiltelefonnummer;
	String epostadresse;
	Integer antallRenotifikasjoner;
	Integer renotifikasjonIntervall;
	String tittel;
	String epostTekst;
	String smsTekst;
	List<Kanal> prefererteKanaler;
}
