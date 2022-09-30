package no.nav.doknotifikasjon.rnot001;

import lombok.Value;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;

import java.time.LocalDateTime;

@Value
public class NotifikasjonDistribusjonDto {
	int id;
	Status status;
	Kanal kanal;
	String kontaktInfo;
	String tittel;
	String tekst;
	LocalDateTime sendtDato;
}
