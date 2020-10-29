package no.nav.doknotifikasjon.knot002.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;

@Getter
@Setter
@Builder
public class DoknotifikasjonSms {
	public String notifikasjonDistribusjonId;
	public String bestillingsId;
	public String bestillerId;
	public Status distribusjonStatus;
	public Kanal kanal;
	public String kontakt;
	public String tekst;
}
