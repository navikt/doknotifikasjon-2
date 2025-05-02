package no.nav.doknotifikasjon.knot003.itest.utils;

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.mockito.ArgumentMatcher;

public class DoknotifikasjonStatusMatcher implements ArgumentMatcher<DoknotifikasjonStatus> {

	private final DoknotifikasjonStatus left;

	public DoknotifikasjonStatusMatcher(String bestillerId, String bestillingsId, String status, String melding, long id) {
		left = DoknotifikasjonStatus.newBuilder()
				.setBestillerId(bestillerId)
				.setBestillingsId(bestillingsId)
				.setStatus(status)
				.setMelding(melding)
				.setDistribusjonId(id)
				.build();
	}

	@Override
	public boolean matches(DoknotifikasjonStatus right) {
		return right.getBestillerId().equals(left.getBestillerId()) &&
			   right.getBestillingsId().equals(left.getBestillingsId()) &&
			   right.getStatus().equals(left.getStatus()) &&
			   right.getMelding().equals(left.getMelding()) &&
			   right.getDistribusjonId().equals(left.getDistribusjonId());
	}
}