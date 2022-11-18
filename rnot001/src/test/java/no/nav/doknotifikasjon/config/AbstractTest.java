package no.nav.doknotifikasjon.config;

import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.ApplicationTestConfig;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Transactional
@EnableMockOAuth2Server
@ActiveProfiles("itest")
@SpringBootTest(classes = {AbstractTest.TestConfig.class, ApplicationTestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AbstractTest {

	@Configuration
	public static class TestConfig {
		@Bean
		@Primary
		ClientHttpRequestFactory clientHttpRequestFactoryTest() {
			return new SimpleClientHttpRequestFactory();
		}
	}

	protected static final String SERVICE_USER_ID = "srrServiceUser";

	@Autowired
	public NotifikasjonRepository notifikasjonRepository;

	@Autowired
	public NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@Autowired
	protected MockOAuth2Server server;

	@Autowired
	protected TestRestTemplate restTemplate;

	public void commitAndBeginNewTransaction() {
		TestTransaction.flagForCommit();
		TestTransaction.end();
		TestTransaction.start();
	}

	public void commitTransaction() {
		TestTransaction.flagForCommit();
		TestTransaction.end();
	}

	protected HttpHeaders azureHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.setBearerAuth(getHeaderToken(SERVICE_USER_ID));
		return headers;
	}

	private String getHeaderToken(String serviceUser) {
		return jwt(serviceUser, new HashMap<>());
	}

	protected String jwt(String subject, Map<String, Object> claims) {
		String issuerId = "azurev2";
		String audience = "gosys";
		return server.issueToken(
				issuerId,
				"gosys-clientid",
				new DefaultOAuth2TokenCallback(
						issuerId,
						subject,
						"JWT",
						List.of(audience),
						claims,
						60
				)
		).serialize();
	}
}