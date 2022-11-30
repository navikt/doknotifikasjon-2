package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static no.nav.doknotifikasjon.TestUtils.KONTAKTINFO;
import static no.nav.doknotifikasjon.TestUtils.SENDT_DATO;
import static no.nav.doknotifikasjon.TestUtils.TEKST;
import static no.nav.doknotifikasjon.TestUtils.TITTEL;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjonDistribusjonWithId;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjonWithId;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class Rnot001MapperTest {

	@Test
	public void shouldMap() {
		Notifikasjon notifikasjon = createNotifikasjonWithId();
		Set<NotifikasjonDistribusjon> distribusjoner = new HashSet<>();
		distribusjoner.add(createNotifikasjonDistribusjonWithId(notifikasjon, SMS, 1));
		distribusjoner.add(createNotifikasjonDistribusjonWithId(notifikasjon, EPOST, 2));
		notifikasjon.setNotifikasjonDistribusjon(distribusjoner);

		NotifikasjonInfoTo notifikasjonInfoTo = Rnot001Mapper.mapNotifikasjon(notifikasjon);

		assertThat(notifikasjonInfoTo.id(), is(notifikasjon.getId()));
		assertThat(notifikasjonInfoTo.bestillerId(), is(notifikasjon.getBestillerId()));
		assertThat(notifikasjonInfoTo.status(), is(notifikasjon.getStatus()));
		assertThat(notifikasjonInfoTo.antallRenotifikasjoner(), is(notifikasjon.getAntallRenotifikasjoner()));
		assertThat(notifikasjonInfoTo.notifikasjonDistribusjoner().size(), is(notifikasjon.getNotifikasjonDistribusjon().size()));
		List<NotifikasjonDistribusjonDto> distribusjonDtos = notifikasjonInfoTo.notifikasjonDistribusjoner().stream()
				.sorted(Comparator.comparing(NotifikasjonDistribusjonDto::id)).toList();

		assertThat(notifikasjonInfoTo.notifikasjonDistribusjoner().size(), is(2));
		assertNotifikasjonDistribusjon(distribusjonDtos.get(0), 1, SMS, null);
		assertNotifikasjonDistribusjon(distribusjonDtos.get(1), 2, EPOST, TITTEL);
	}

	private void assertNotifikasjonDistribusjon(NotifikasjonDistribusjonDto notifDistDto, int id, Kanal kanal, String tittel) {
		assertThat(notifDistDto.id(), is(id));
		assertThat(notifDistDto.status(), is(Status.FERDIGSTILT));
		assertThat(notifDistDto.kanal(), is(kanal));
		assertThat(notifDistDto.kontaktInfo(), is(KONTAKTINFO));
		assertThat(notifDistDto.tittel(), is(tittel));
		assertThat(notifDistDto.tekst(), is(TEKST));
		assertThat(notifDistDto.sendtDato(), is(SENDT_DATO));
	}

}
