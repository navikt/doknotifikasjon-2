package no.nav.doknotifikasjon.consumer.dkif;

public interface DigitalKontaktinformasjon {
	DigitalKontaktinformasjonTo.DigitalKontaktinfo hentDigitalKontaktinfo(final String personident);
}
