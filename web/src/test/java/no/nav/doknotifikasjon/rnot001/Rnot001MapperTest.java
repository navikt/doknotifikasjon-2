package no.nav.doknotifikasjon.rnot001;

import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static no.nav.doknotifikasjon.rnot001.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.rnot001.TestUtils.createNotifikasjonWithDistribusjon;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class Rnot001MapperTest {

	@Test
	public void shouldMap(){
		LocalDateTime now = LocalDateTime.now();
		Notifikasjon notifikasjon = createNotifikasjon();
		NotifikasjonDistribusjon notDist = createNotifikasjonWithDistribusjon(notifikasjon, now);

		NotifikasjonInfoTo notifikasjonInfoTo = Rnot001Mapper.mapNotifikasjonDistribusjon(Arrays.asList(notDist));

		assertThat(notifikasjonInfoTo.getId(), is(notifikasjon.getId()));
		assertThat(notifikasjonInfoTo.getBestillerId(), is(notifikasjon.getBestillerId()));
		assertThat(notifikasjonInfoTo.getStatus(), is(notifikasjon.getStatus()));
		assertThat(notifikasjonInfoTo.getAntallRenotifikasjoner(), is(notifikasjon.getAntallRenotifikasjoner()));
		assertThat(notifikasjonInfoTo.getNotifikasjonDistribusjoner().size(), is(notifikasjon.getNotifikasjonDistribusjon().size()));

		assertThat(notifikasjonInfoTo.getNotifikasjonDistribusjoner().size(), is(1));
		NotifikasjonDistribusjonDto notiDto = notifikasjonInfoTo.getNotifikasjonDistribusjoner().get(0);
	}

}
