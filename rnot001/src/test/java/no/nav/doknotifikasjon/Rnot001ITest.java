package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.config.AbstractTest;
import no.nav.doknotifikasjon.model.Notifikasjon;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static no.nav.doknotifikasjon.TestUtils.BESTILLINGS_ID;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjonDistribusjon;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;

public class Rnot001ITest extends AbstractTest {

	private static final String RNOT001_BASE_URL = "/rest/v1/notifikasjoninfo/";
	private static final String BAD_BESTILLINGS_ID = "bare_tull";

	@Test
	public void shouldGetBestilling() {
		LocalDateTime now = LocalDateTime.now();
		Notifikasjon notifikasjon = createNotifikasjon();
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjon(notifikasjon, now, SMS));
		notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjon(notifikasjon, now, EPOST));
		notifikasjonRepository.save(notifikasjon);
		commitAndBeginNewTransaction();


		HttpEntity<String> requestHttpEntity = new HttpEntity<>("", azureHeaders());
		ResponseEntity<NotifikasjonInfoTo> response = restTemplate.exchange(
				RNOT001_BASE_URL + BESTILLINGS_ID, GET, requestHttpEntity, NotifikasjonInfoTo.class);

		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		NotifikasjonInfoTo notifikasjonInfoTo = response.getBody();
		assertThat(notifikasjonInfoTo.notifikasjonDistribusjoner().size(), is(2));

	}

	@Test
	public void shouldGetNoneWhenBadBestillingsId() {
		HttpEntity<String> requestHttpEntity = new HttpEntity<>("", azureHeaders());
		ResponseEntity<String> response = restTemplate.exchange(
				RNOT001_BASE_URL + BAD_BESTILLINGS_ID, GET, requestHttpEntity, String.class);

		assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
	}

	@Test
	public void shouldGetNoneWhenBadBearerToken() {
		HttpEntity<String> requestHttpEntity = new HttpEntity<>("", badHeaders());
		ResponseEntity<String> response = restTemplate.exchange(
				RNOT001_BASE_URL + BAD_BESTILLINGS_ID, GET, requestHttpEntity, String.class);

		assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
	}

	protected HttpHeaders badHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.setBearerAuth("BAD_TOKEN");
		return headers;
	}

}
