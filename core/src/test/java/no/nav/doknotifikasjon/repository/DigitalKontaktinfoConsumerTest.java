package no.nav.doknotifikasjon.repository;

import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.doknotifikasjon.exception.technical.DigitalKontaktinformasjonTechnicalException;
import no.nav.doknotifikasjon.repository.utils.ApplicationTestConfig;
import no.nav.doknotifikasjon.repository.utils.STSTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import wiremock.org.apache.http.HttpHeaders;
import wiremock.org.apache.http.entity.ContentType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {ApplicationTestConfig.class, STSTestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext()
@ActiveProfiles({"itest", "itestWeb"})
class DigitalKontaktinfoConsumerTest {

	private static final String FNR = "12345678911";
	private static final String MOBIL = "+4799999999";
	private static final String EPOST = "epost";

	@Autowired
	private DigitalKontaktinfoConsumer digitalKontaktinfoConsumer;

	@BeforeEach
	public void setup() {
		stubSecurityToken();
	}

	@Test
	void shouldReturnKontaktinfoWhenFullResponseFromDkif() {
		stubDkifWithBodyFile("dkif/dkif-full-response.json");

		DigitalKontaktinformasjonTo digitalKontaktinfo = digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FNR);
		DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinfo = digitalKontaktinfo.getKontaktinfo().get(FNR);

		assertEquals(EPOST, kontaktinfo.getEpostadresse());
		assertEquals(MOBIL, kontaktinfo.getMobiltelefonnummer());
		assertTrue(kontaktinfo.isKanVarsles());
		assertFalse(kontaktinfo.isReservert());
	}

	@Test
	void shouldReturnKontaktinfoWhenOnlyKontaktinfoFromDkif() {
		stubDkifWithBodyFile("dkif/dkif-kontaktinfo-response.json");

		DigitalKontaktinformasjonTo digitalKontaktinfo = digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FNR);
		DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinfo = digitalKontaktinfo.getKontaktinfo().get(FNR);

		assertEquals(EPOST, kontaktinfo.getEpostadresse());
		assertEquals(MOBIL, kontaktinfo.getMobiltelefonnummer());
		assertTrue(kontaktinfo.isKanVarsles());
		assertFalse(kontaktinfo.isReservert());
	}

	@Test
	void shouldReturnNullWhenFeilFromDkif() {
		stubDkifWithBodyFile("dkif/dkif-feil.json");
		DigitalKontaktinformasjonTo digitalKontaktinformasjon = digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FNR);

		assertNull(digitalKontaktinformasjon.getKontaktinfo());
		assertEquals("Ingen kontaktinformasjon er registrert på personen", digitalKontaktinformasjon.getFeil().get(FNR).getMelding());
	}

	private void stubSecurityToken() {
		stubFor(get("/securitytoken?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts-response.json")));
	}

	private void stubDkifWithBodyFile(String bodyfile) {
		stubFor(get(urlEqualTo("/dkif/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false"))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile(bodyfile)));
	}
}
