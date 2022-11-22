package no.nav.doknotifikasjon;


import no.nav.doknotifikasjon.repository.utils.ApplicationTestConfig;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.HashMap;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
		classes = {ApplicationTestConfig.class},
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
@ActiveProfiles({"itest"})
public class Rnot002ITest {

	private static final String PERSONIDENT = "12345678901";

	@Autowired
	WebTestClient webTestClient;

	@Autowired
	protected MockOAuth2Server server;

	@BeforeEach
	void setup() {
		stubAzure();
	}

	@Test
	void shouldReturnKanVarsles() {
		stubDigdirKRRProxyWithBodyFile("digdir_krr_proxy_happy.json");
		stubSikkerhetsnivaaWithBodyFile("sikkerhetsnivaa3.json");

		var response = getKanVarsles();

		assertNotNull(response);
		assertTrue(response.kanVarsles());
		assertEquals(3, response.sikkerhetsnivaa());
	}

	@Test
	void shouldReturnKanVarslesFalseOnFeilFromDigdir() {
		stubDigdirKRRProxyWithBodyFile("digdir_krr_proxy_person_ikke_funnet.json");

		var response = getKanVarsles();

		assertNotNull(response);
		assertFalse(response.kanVarsles());
		verify(0, getRequestedFor(urlEqualTo("/sikkerhetsnivaa")));
		assertEquals(3, response.sikkerhetsnivaa());
	}

	@Test
	void shouldReturnSikkerhetsnivaa4() {
		stubDigdirKRRProxyWithBodyFile("digdir_krr_proxy_happy.json");
		stubSikkerhetsnivaaWithBodyFile("sikkerhetsnivaa4.json");

		var response = getKanVarsles();

		assertNotNull(response);
		assertTrue(response.kanVarsles());
		assertEquals(4, response.sikkerhetsnivaa());
	}

	private KanVarslesResponse getKanVarsles() {
		return webTestClient
				.get()
				.uri("/rest/v1/kanvarsles/" + PERSONIDENT)
				.headers((headers) -> headers.setBearerAuth(jwt()))
				.exchange()
				.expectStatus().isOk()
				.returnResult(KanVarslesResponse.class)
				.getResponseBody()
				.blockFirst();
	}

	protected String jwt() {
		String issuerId = "azurev2";
		String audience = "gosys";
		return server.issueToken(
				issuerId,
				"gosys-clientid",
				new DefaultOAuth2TokenCallback(
						issuerId,
						"subject",
						"JWT",
						List.of(audience),
						new HashMap<>(),
						60
				)
		).serialize();
	}

	private void stubAzure() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
	}

	private void stubDigdirKRRProxyWithBodyFile(String bodyfile) {
		stubFor(post(urlEqualTo("/digdir_krr_proxy/rest/v1/personer"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(bodyfile)));
	}

	private void stubSikkerhetsnivaaWithBodyFile(String bodyfile) {
		stubFor(post("/sikkerhetsnivaa").willReturn(aResponse().withStatus(OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile(bodyfile)));
	}
}
