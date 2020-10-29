package no.nav.doknotifikasjon.consumer.test;

import no.nav.doknotifikasjon.consumer.DoknotifikasjonMapper;
import no.nav.doknotifikasjon.consumer.DoknotifikasjonTO;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.junit.jupiter.api.Test;

import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjon;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DoknotifikasjonMapperTest {

	private final DoknotifikasjonMapper doknotifikasjonMapper = new DoknotifikasjonMapper();

	@Test
	void shouldMap() {
		Doknotifikasjon doknotifikasjon = createDoknotifikasjon();

		DoknotifikasjonTO doknotifikasjonTo = doknotifikasjonMapper.map(createDoknotifikasjon());

		assertEquals(doknotifikasjon.getBestillingsId(), doknotifikasjonTo.getBestillingsId());
		assertEquals(doknotifikasjon.getBestillerId(), doknotifikasjonTo.getBestillerId());

		assertEquals(doknotifikasjon.getFodselsnummer(), doknotifikasjonTo.getFodselsnummer());
		assertEquals(doknotifikasjon.getAntallRenotifikasjoner(), doknotifikasjonTo.getAntallRenotifikasjoner());
		assertEquals(doknotifikasjon.getRenotifikasjonIntervall(), doknotifikasjonTo.getRenotifikasjonIntervall());
		assertEquals(doknotifikasjon.getTittel(), doknotifikasjonTo.getTittel());
		assertEquals(doknotifikasjon.getSmsTekst(), doknotifikasjonTo.getSmsTekst());
		assertEquals(doknotifikasjon.getEpostTekst(), doknotifikasjonTo.getEpostTekst());
		assertEquals(doknotifikasjon.getPrefererteKanaler().size(), doknotifikasjonTo.getPrefererteKanaler().size());
		assertEquals(doknotifikasjon.getPrefererteKanaler().get(0).toString(), doknotifikasjonTo.getPrefererteKanaler().get(0).toString());
		assertEquals(doknotifikasjon.getPrefererteKanaler().get(1).toString(), doknotifikasjonTo.getPrefererteKanaler().get(1).toString());
	}
}