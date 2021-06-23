package no.nav.doknotifikasjon.consumer.test;

import no.nav.doknotifikasjon.consumer.DoknotifikasjonMedKontaktInfoMapper;
import no.nav.doknotifikasjon.consumer.NotifikasjonMedKontaktInfoTO;
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo;
import org.junit.jupiter.api.Test;

import static no.nav.doknotifikasjon.consumer.TestUtils.createNotifikasjon;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DoknotifikasjonMedKontaktInfoMapperTest {

	private final DoknotifikasjonMedKontaktInfoMapper doknotifikasjonMedKontaktInfoMapper = new DoknotifikasjonMedKontaktInfoMapper();

	@Test
	void shouldMap() {
		NotifikasjonMedkontaktInfo doknotifikasjon = createNotifikasjon();

		NotifikasjonMedKontaktInfoTO notifikasjonMedKontaktInfoTo = doknotifikasjonMedKontaktInfoMapper.map(createNotifikasjon());

		assertEquals(doknotifikasjon.getBestillingsId(), notifikasjonMedKontaktInfoTo.getBestillingsId());
		assertEquals(doknotifikasjon.getBestillerId(), notifikasjonMedKontaktInfoTo.getBestillerId());

		assertEquals(doknotifikasjon.getFodselsnummer(), notifikasjonMedKontaktInfoTo.getFodselsnummer());
		assertEquals(doknotifikasjon.getAntallRenotifikasjoner(), notifikasjonMedKontaktInfoTo.getAntallRenotifikasjoner());
		assertEquals(doknotifikasjon.getRenotifikasjonIntervall(), notifikasjonMedKontaktInfoTo.getRenotifikasjonIntervall());
		assertEquals(doknotifikasjon.getTittel(), notifikasjonMedKontaktInfoTo.getTittel());
		assertEquals(doknotifikasjon.getSmsTekst(), notifikasjonMedKontaktInfoTo.getSmsTekst());
		assertEquals(doknotifikasjon.getEpostTekst(), notifikasjonMedKontaktInfoTo.getEpostTekst());
		assertEquals(doknotifikasjon.getPrefererteKanaler().size(), notifikasjonMedKontaktInfoTo.getPrefererteKanaler().size());
		assertEquals(doknotifikasjon.getPrefererteKanaler().get(0).toString(), notifikasjonMedKontaktInfoTo.getPrefererteKanaler().get(0).toString());
		assertEquals(doknotifikasjon.getPrefererteKanaler().get(1).toString(), notifikasjonMedKontaktInfoTo.getPrefererteKanaler().get(1).toString());
	}
}