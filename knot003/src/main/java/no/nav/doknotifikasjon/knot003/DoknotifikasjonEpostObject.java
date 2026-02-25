package no.nav.doknotifikasjon.knot003;

import lombok.Builder;
import lombok.Value;
import no.nav.doknotifikasjon.consumer.altinn.DoknotifikasjonDistributableInChannel;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;

@Value
@Builder
public class DoknotifikasjonEpostObject implements DoknotifikasjonDistributableInChannel {
	long notifikasjonDistribusjonId;
	String bestillingsId;
	String bestillerId;
	Status distribusjonStatus;
	Kanal kanal;
	String kontaktInfo;
	String tekst;
	String tittel;
	String fodselsnummer;
}
