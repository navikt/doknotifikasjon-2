package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.springframework.stereotype.Component;

@Component
public class DoknotifikasjonStatusMapper {

	public DoknotifikasjonStatusTo map(DoknotifikasjonStatus doknotifikasjonStatus) {
		return DoknotifikasjonStatusTo.builder()
				.bestillerId(doknotifikasjonStatus.getBestillerId())
				.bestillingsId(doknotifikasjonStatus.getBestillingsId())
				.status(Status.valueOf(doknotifikasjonStatus.getStatus()))
				.melding(doknotifikasjonStatus.getMelding())
				.distribusjonId(doknotifikasjonStatus.getDistribusjonId())
				.build();
	}
}
