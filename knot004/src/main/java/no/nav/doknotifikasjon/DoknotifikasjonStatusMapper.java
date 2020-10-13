package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;

public class DoknotifikasjonStatusMapper {

	public DoknotifikasjonStatusDto map(DoknotifikasjonStatus doknotifikasjonStatus){
		return new DoknotifikasjonStatusDto().builder()
				.bestillerId(doknotifikasjonStatus.getBestillerId())
				.bestillingId(doknotifikasjonStatus.getBestillingsId())
				.status(doknotifikasjonStatus.getStatus())
				.melding(doknotifikasjonStatus.getMelding())
				.distribusjonId(doknotifikasjonStatus.getDistribusjonId())
				.build();
	}
}
