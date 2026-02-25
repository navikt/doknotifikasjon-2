package no.nav.doknotifikasjon.consumer.altinn;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;

public interface DoknotifikasjonDistributableInChannel {
	long getNotifikasjonDistribusjonId();
	String getBestillingsId();
	String getBestillerId();
	Status getDistribusjonStatus();
	Kanal getKanal();
}
