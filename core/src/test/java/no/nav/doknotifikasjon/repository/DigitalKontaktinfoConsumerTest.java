package no.nav.doknotifikasjon.repository;

import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.DigitalKontaktinformasjonTo.DigitalKontaktinfo;
import no.nav.doknotifikasjon.repository.utils.ApplicationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@SpringBootTest(
		classes = {ApplicationTestConfig.class},
		webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles({"itest", "wiremock"})
class DigitalKontaktinfoConsumerTest {

	private static final String FNR = "12345678911";
	private static final String MOBIL = "+4799999999";
	private static final String EPOST = "epost";

	@MockBean
	INotificationAgencyExternalBasic iNotificationAgencyExternalBasic;

	@Autowired
	private DigitalKontaktinfoConsumer digitalKontaktinfoConsumer;

	@BeforeEach
	public void setup() {
		stubAzure();
	}

	@Test
	void shouldReturnKontaktinfoWhenFullResponseFromDigdirKRRProxy() {
		stubDigdirKRRProxyWithBodyFile("digdir/digdir_krr_proxy-full-response.json");

		DigitalKontaktinformasjonTo digitalKontaktinfo = digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FNR);
		DigitalKontaktinfo kontaktinfo = digitalKontaktinfo.personer().get(FNR);

		assertEquals(EPOST, kontaktinfo.epostadresse());
		assertEquals(MOBIL, kontaktinfo.mobiltelefonnummer());
		assertTrue(kontaktinfo.kanVarsles());
		assertFalse(kontaktinfo.reservert());
	}

	@Test
	void shouldReturnKontaktinfoWhenOnlyKontaktinfoFromDigdirKRRProxy() {
		stubDigdirKRRProxyWithBodyFile("digdir/digdir_krr_proxy-kontaktinfo-response.json");

		DigitalKontaktinformasjonTo digitalKontaktinfo = digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FNR);
		DigitalKontaktinfo kontaktinfo = digitalKontaktinfo.personer().get(FNR);

		assertEquals(EPOST, kontaktinfo.epostadresse());
		assertEquals(MOBIL, kontaktinfo.mobiltelefonnummer());
		assertTrue(kontaktinfo.kanVarsles());
		assertFalse(kontaktinfo.reservert());
	}

	@Test
	void shouldReturnNullWhenFeilFromDigdirKRRProxy() {
		stubDigdirKRRProxyWithBodyFile("digdir/digdir_krr_proxy-feil.json");
		DigitalKontaktinformasjonTo digitalKontaktinformasjon = digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FNR);

		assertNull(digitalKontaktinformasjon.personer());
		assertEquals("Ingen kontaktinformasjon er registrert på personen", digitalKontaktinformasjon.feil().get(FNR));
	}

	private void stubDigdirKRRProxyWithBodyFile(String bodyfile) {
		stubFor(post(urlEqualTo("/digdir_krr_proxy/rest/v1/personer"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(bodyfile)));
	}

	private void stubAzure() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
	}
}
