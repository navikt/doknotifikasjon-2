package no.nav.doknotifikasjon.knot003;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;

@Value
@Builder
public class DoknotifikasjonEpostTo {
	String notifikasjonDistribusjonId;
	String bestillingsId;
	String bestillerId;
	Status distribusjonStatus;
	Kanal kanal;
	String kontakt;
	String tekst;
	String tittel;
	String fodselsnummer;
}
