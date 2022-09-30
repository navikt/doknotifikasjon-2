package no.nav.doknotifikasjon.rnot001;

import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.rnot001.TestUtils.TITTEL;
import static no.nav.doknotifikasjon.rnot001.TestUtils.createNotifikasjonDistribusjonWithId;
import static no.nav.doknotifikasjon.rnot001.TestUtils.createNotifikasjonWithId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class Rnot001MapperTest {

	@Test
	public void shouldMap() {
		LocalDateTime now = LocalDateTime.now();
		Notifikasjon notifikasjon = createNotifikasjonWithId();
		NotifikasjonDistribusjon notDist = createNotifikasjonDistribusjonWithId(notifikasjon, now, SMS, 1);
		NotifikasjonDistribusjon notdist2 = createNotifikasjonDistribusjonWithId(notifikasjon, now, EPOST, 2);
		notifikasjon.setNotifikasjonDistribusjon(Set.of(notDist, notdist2));

		NotifikasjonInfoTo notifikasjonInfoTo = Rnot001Mapper.mapNotifikasjon(notifikasjon);

		assertThat(notifikasjonInfoTo.getId(), is(notifikasjon.getId()));
		assertThat(notifikasjonInfoTo.getBestillerId(), is(notifikasjon.getBestillerId()));
		assertThat(notifikasjonInfoTo.getStatus(), is(notifikasjon.getStatus()));
		assertThat(notifikasjonInfoTo.getAntallRenotifikasjoner(), is(notifikasjon.getAntallRenotifikasjoner()));
		assertThat(notifikasjonInfoTo.getNotifikasjonDistribusjoner().size(), is(notifikasjon.getNotifikasjonDistribusjon().size()));

		NotifikasjonDistribusjonDto notiDto1;
		NotifikasjonDistribusjonDto notiDto2;
		List<NotifikasjonDistribusjonDto> distribusjoner = notifikasjonInfoTo.getNotifikasjonDistribusjoner();
		if (distribusjoner.get(0).getId() == 1) {
			notiDto1 = distribusjoner.get(0);
			notiDto2 = distribusjoner.get(1);
		} else {
			notiDto1 = distribusjoner.get(1);
			notiDto2 = distribusjoner.get(0);
		}
		assertThat(notifikasjonInfoTo.getNotifikasjonDistribusjoner().size(), is(2));
		assertNotifikasjonDistribusjon(notiDto1, notDist, null);
		assertNotifikasjonDistribusjon(notiDto2, notdist2, TITTEL);
	}

	private void assertNotifikasjonDistribusjon(NotifikasjonDistribusjonDto notifDistDto, NotifikasjonDistribusjon notifDist, String tittel) {
		assertThat(notifDistDto.getId(), is(notifDist.getId()));
		assertThat(notifDistDto.getStatus(), is(notifDist.getStatus()));
		assertThat(notifDistDto.getKanal(), is(notifDist.getKanal()));
		assertThat(notifDistDto.getKontaktInfo(), is(notifDist.getKontaktInfo()));
		assertThat(notifDistDto.getTittel(), is(tittel));
		assertThat(notifDistDto.getTekst(), is(notifDist.getTekst()));
		assertThat(notifDistDto.getSendtDato(), is(notifDist.getSendtDato()));
	}

}
