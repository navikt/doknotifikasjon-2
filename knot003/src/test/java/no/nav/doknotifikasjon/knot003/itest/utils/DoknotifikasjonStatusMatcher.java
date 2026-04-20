package no.nav.doknotifikasjon.knot003.itest.utils;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.mockito.ArgumentMatcher;

public class DoknotifikasjonStatusMatcher implements ArgumentMatcher<DoknotifikasjonStatus> {

	private final DoknotifikasjonStatus left;

	public DoknotifikasjonStatusMatcher(String bestillerId, String bestillingsId, String status, String melding, long id, Kanal kanal) {
		left = DoknotifikasjonStatus.newBuilder()
				.setBestillerId(bestillerId)
				.setBestillingsId(bestillingsId)
				.setStatus(status)
				.setMelding(melding)
				.setDistribusjonId(id)
				.setKanal(kanal.name())
				.build();
	}

	@Override
	public boolean matches(DoknotifikasjonStatus right) {
		return right.getBestillerId().equals(left.getBestillerId()) &&
			   right.getBestillingsId().equals(left.getBestillingsId()) &&
			   right.getStatus().equals(left.getStatus()) &&
			   right.getMelding().equals(left.getMelding()) &&
			   right.getDistribusjonId().equals(left.getDistribusjonId()) &&
			   right.getKanal() != null && right.getKanal().equals(left.getKanal());
	}
}