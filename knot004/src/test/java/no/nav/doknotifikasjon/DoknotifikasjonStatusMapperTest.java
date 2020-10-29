package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.junit.jupiter.api.Test;

import static no.nav.doknotifikasjon.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DoknotifikasjonStatusMapperTest {

	private final DoknotifikasjonStatusMapper doknotifikasjonStatusMapper = new DoknotifikasjonStatusMapper();

	@Test
	void shouldMap() {
		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLINGS_ID, BESTILLER_ID, STATUS_OPPRETTET.toString(), MELDING, DISTRIBUSJON_ID);

		DoknotifikasjonStatusTo doknotifikasjonStatusTo = doknotifikasjonStatusMapper.map(doknotifikasjonStatus);

		assertEquals(doknotifikasjonStatus.getBestillingsId(), doknotifikasjonStatusTo.getBestillingsId());
		assertEquals(doknotifikasjonStatus.getBestillerId(), doknotifikasjonStatusTo.getBestillerId());
		assertEquals(doknotifikasjonStatus.getMelding(), doknotifikasjonStatusTo.getMelding());
		assertEquals(Status.valueOf(doknotifikasjonStatus.getStatus()), doknotifikasjonStatusTo.getStatus());
		assertEquals(doknotifikasjonStatus.getDistribusjonId(), doknotifikasjonStatusTo.getDistribusjonId());
	}
}