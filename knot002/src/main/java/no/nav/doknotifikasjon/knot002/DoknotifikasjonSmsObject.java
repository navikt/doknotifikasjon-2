package no.nav.doknotifikasjon.knot002;

import lombok.Builder;
import lombok.Value;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;

@Value
@Builder
public class DoknotifikasjonSmsObject {
	long notifikasjonDistribusjonId;
	String bestillingsId;
	String bestillerId;
	Status distribusjonStatus;
	Kanal kanal;
	String kontaktInfo;
	String tekst;
	String fodselsnummer;
}
