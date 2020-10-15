package no.nav.doknotifikasjon;

import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLER_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.BESTILLING_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.DISTRIBUSJON_ID;
import static no.nav.doknotifikasjon.utils.TestUtils.MELDING;
import static no.nav.doknotifikasjon.utils.TestUtils.STATUS_OPPRETTET;
import static org.junit.jupiter.api.Assertions.assertEquals;

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.junit.jupiter.api.Test;

class DoknotifikasjonStatusMapperTest {

	private final DoknotifikasjonStatusMapper doknotifikasjonStatusMapper = new DoknotifikasjonStatusMapper();

	@Test
	void shouldMap() {
		DoknotifikasjonStatus doknotifikasjonStatus = new DoknotifikasjonStatus(BESTILLING_ID, BESTILLER_ID, STATUS_OPPRETTET, MELDING, DISTRIBUSJON_ID);

		DoknotifikasjonStatusTo doknotifikasjonStatusTo = doknotifikasjonStatusMapper.map(new DoknotifikasjonStatus(BESTILLING_ID, BESTILLER_ID, STATUS_OPPRETTET, MELDING, DISTRIBUSJON_ID));

		assertEquals(doknotifikasjonStatus.getBestillingsId(), doknotifikasjonStatusTo.getBestillingId());
		assertEquals(doknotifikasjonStatus.getBestillerId(), doknotifikasjonStatusTo.getBestillerId());
		assertEquals(doknotifikasjonStatus.getMelding(), doknotifikasjonStatusTo.getMelding());
		assertEquals(doknotifikasjonStatus.getStatus(), doknotifikasjonStatusTo.getStatus());
		assertEquals(doknotifikasjonStatus.getDistribusjonId(), doknotifikasjonStatusTo.getDistribusjonId());
	}
}