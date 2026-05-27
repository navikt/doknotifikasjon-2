package no.nav.doknotifikasjon.consumer;

import no.nav.doknotifikasjon.config.properties.Altinn3Props;
import no.nav.doknotifikasjon.consumer.altinn3.Altinn3TokenExchangeConsumer;
import no.nav.doknotifikasjon.consumer.altinn3.NaisTexasConsumer;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class Altinn3TokenExchangeConsumerTest {

	private static final Altinn3Props ALTINN_PROPS = new Altinn3Props("exchange-token", "order-notification");

	private MockRestServiceServer mockRestServiceServer;
	private RestClient.Builder restClientBuilder;
	@Mock
	NaisTexasConsumer naisTexasConsumer;

	Altinn3TokenExchangeConsumer altinn3TokenExchangeConsumer;

	@BeforeEach
	void setUp() {
		restClientBuilder = RestClient.builder();
		mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder).build();
		when(naisTexasConsumer.getMaskinportenToken()).thenReturn("token");

		altinn3TokenExchangeConsumer = new Altinn3TokenExchangeConsumer(naisTexasConsumer, new JsonMapper(), restClientBuilder, ALTINN_PROPS);
	}

	@Test
	void returnsParsedAltinnToken() {
		mockRestServiceServer.expect(requestTo("exchange-token"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer token"))
			.andRespond(withSuccess("\"altinnToken\"", MediaType.APPLICATION_JSON));

		String token = altinn3TokenExchangeConsumer.getAltinnToken();

		assertThat(token).isEqualTo("altinnToken");
	}

	@Test
	void throwAltinnTechnicalExceptionOnInvalidToken() {
		mockRestServiceServer.expect(requestTo("exchange-token"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer token"))
			.andRespond(withSuccess("altinnTokenUgyldigFormat", MediaType.APPLICATION_JSON));

		AltinnTechnicalException exceptionThrown = assertThrows(AltinnTechnicalException.class, altinn3TokenExchangeConsumer::getAltinnToken);
		assertThat(exceptionThrown.getMessage()).contains("Kunne ikke parse token");
	}
}
