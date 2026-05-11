package no.nav.doknotifikasjon.knot002.itest.utils;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.jspecify.annotations.Nullable;
import org.mockito.ArgumentMatcher;

import java.util.Objects;

public class DoknotifikasjonStatusMatcher implements ArgumentMatcher<DoknotifikasjonStatus> {

	private final DoknotifikasjonStatus left;

	public DoknotifikasjonStatusMatcher(String bestillerId, String bestillingsId, String status, String melding, long id, @Nullable Kanal kanal) {
		left = DoknotifikasjonStatus.newBuilder()
				.setBestillerId(bestillerId)
				.setBestillingsId(bestillingsId)
				.setStatus(status)
				.setMelding(melding)
				.setDistribusjonId(id)
				.setKanal(kanal != null ? kanal.name() : null)
				.build();
	}

	@Override
	public boolean matches(DoknotifikasjonStatus right) {
		return right.getBestillerId().equals(left.getBestillerId()) &&
			   right.getBestillingsId().equals(left.getBestillingsId()) &&
			   right.getStatus().equals(left.getStatus()) &&
			   right.getMelding().equals(left.getMelding()) &&
			   right.getDistribusjonId().equals(left.getDistribusjonId()) &&
			   Objects.equals(right.getKanal(), left.getKanal());
	}
}