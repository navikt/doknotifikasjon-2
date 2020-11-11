package no.nav.doknotifikasjon.knot002;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Knot002Mapper {

	public DoknotifikasjonSmsTo mapNotifikasjonDistrubisjon(NotifikasjonDistribusjon notifikasjonDistribusjon, Notifikasjon notifikasjon) {
			return DoknotifikasjonSmsTo.builder()
					.notifikasjonDistribusjonId(String.valueOf(notifikasjonDistribusjon.getId()))
					.bestillerId(notifikasjon.getBestillerId())
					.bestillingsId(notifikasjon.getBestillingsId())
					.distribusjonStatus(notifikasjonDistribusjon.getStatus())
					.kanal(notifikasjonDistribusjon.getKanal())
					.kontakt(notifikasjonDistribusjon.getKontaktInfo())
					.tekst(notifikasjonDistribusjon.getTekst())
					.fodselsnummer(notifikasjon.getMottakerId())
					.build();
	}
}
