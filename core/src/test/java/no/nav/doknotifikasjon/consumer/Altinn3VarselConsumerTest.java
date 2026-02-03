package no.nav.doknotifikasjon.consumer;

import no.nav.doknotifikasjon.config.properties.Altinn3Props;
import no.nav.doknotifikasjon.consumer.altinn3.Altinn3VarselConsumer;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.RestClient;

import java.util.UUID;
import java.util.stream.Stream;

import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

@RestClientTest(Altinn3VarselConsumer.class)
public class Altinn3VarselConsumerTest {

	private static final String FOEDSELSNUMMER = "01010156798";
	private static final String EPOST_TITTEL = "You've got mail!";
	private static final String EPOST_TEKST = "Dette er innholdet i en epost.";
	private static final String EPOST_ADRESSE = "epost@mottaker.no";
	private static final String TELEFONNUMMER = "+4712349876";
	private static final String SMS_TEKST = "Viktig melding fra NAV";
	private static final String MOCKED_URL = "url";
	private static final String ERROR_TITLE = "NOT-00001";
	private static final String ERROR_MESSAGE = "Ugyldig norsk mobiltelefonnummer.";
	private static final Altinn3Props PROPS_FOR_TEST = new Altinn3Props(MOCKED_URL);

	@Autowired
	MockRestServiceServer mockRestServiceServer;
	@Autowired
	RestClient restClient;

	@BeforeEach
	void setUp() {
		mockRestServiceServer.reset();
	}

	@Test
	void serviceShouldSendEmailToAltinn() {
		String bestillingsId = UUID.randomUUID().toString();
		String kontaktinformasjon = EPOST_ADRESSE;
		String fnr = FOEDSELSNUMMER;
		String tekst = EPOST_TEKST;
		String tittel = EPOST_TITTEL;
		mockRestServiceServer.expect(requestTo("url"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(jsonPath("$.sendersReference").value(bestillingsId))
			.andExpect(jsonPath("$.idempotencyId").value(bestillingsId))
			.andExpect(jsonPath("$.requestedSendTime").doesNotExist())
			.andExpect(jsonPath("$.recipient.recipientEmail.emailSettings.subject").value(tittel))
			.andExpect(jsonPath("$.recipient.recipientEmail.emailSettings.body").value(tekst))
			.andExpect(jsonPath("$.recipient.recipientEmail.emailAddress").value(kontaktinformasjon))
			.andRespond(withSuccess(new ClassPathResource("__files/altinn3/order-notification-ok.json"), MediaType.APPLICATION_JSON));

		Altinn3VarselConsumer consumer = new Altinn3VarselConsumer(restClient, PROPS_FOR_TEST);

		consumer.sendVarsel(EPOST, bestillingsId, kontaktinformasjon, fnr, tekst, tittel);

		mockRestServiceServer.verify();
	}

	@Test
	void serviceShouldSendSmsToAltinn() {
		String bestillingsId = UUID.randomUUID().toString();
		String kontaktinformasjon = TELEFONNUMMER;
		String fnr = FOEDSELSNUMMER;
		String tekst = SMS_TEKST;
		mockRestServiceServer.expect(requestTo("url"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(jsonPath("$.sendersReference").value(bestillingsId))
			.andExpect(jsonPath("$.idempotencyId").value(bestillingsId))
			.andExpect(jsonPath("$.requestedSendTime").doesNotExist())
			.andExpect(jsonPath("$.recipient.recipientSms.smsSettings.body").value(tekst))
			.andExpect(jsonPath("$.recipient.recipientSms.phoneNumber").value(kontaktinformasjon))
			// .andExpect(jsonPath("$.recipient.").value(fnr))
			.andRespond(withSuccess(new ClassPathResource("__files/altinn3/order-notification-ok.json"), MediaType.APPLICATION_JSON));

		Altinn3VarselConsumer consumer = new Altinn3VarselConsumer(restClient, PROPS_FOR_TEST);

		consumer.sendVarsel(SMS, bestillingsId, kontaktinformasjon, fnr, tekst, null);

		mockRestServiceServer.verify();
	}

	@ParameterizedTest
	@MethodSource
	void shouldThrowTechnicalExceptionForProblemsThatAreLikelyRecoverable(Exception expectedException, DefaultResponseCreator response) {
		mockRestServiceServer.expect(requestTo("url"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(response);

		Altinn3VarselConsumer consumer = new Altinn3VarselConsumer(restClient, PROPS_FOR_TEST);

		Exception altinnTechnicalException = assertThrows(expectedException.getClass(), () -> consumer.sendVarsel(EPOST, null, null, null, null, null));

		assertThat(altinnTechnicalException).isInstanceOf(expectedException.getClass());
		assertThat(altinnTechnicalException.getMessage()).isEqualTo(expectedException.getMessage());
	}

	private static Stream<Arguments> shouldThrowTechnicalExceptionForProblemsThatAreLikelyRecoverable() {
		return Stream.of(
			Arguments.of(new AltinnTechnicalException(String.format("Teknisk feil i kall mot Altinn. %s", UNAUTHORIZED), null), withUnauthorizedRequest()),
			Arguments.of(new AltinnTechnicalException(String.format("Teknisk feil i kall mot Altinn. %s", FORBIDDEN), null), withForbiddenRequest()),
			Arguments.of(new AltinnFunctionalException(String.format("Funksjonell feil i kall mot Altinn. %s, errorTitle=%s, errorMessage=%s", BAD_REQUEST, ERROR_TITLE, ERROR_MESSAGE)), withBadRequest().contentType(MediaType.APPLICATION_JSON).body(new ClassPathResource("__files/altinn3/order-notification-notok.json"))),
			Arguments.of(new AltinnFunctionalException(String.format("Funksjonell feil i kall mot Altinn. %s, errorTitle=%s, errorMessage=%s", UNPROCESSABLE_ENTITY, ERROR_TITLE, ERROR_MESSAGE)), withStatus(UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON).body(new ClassPathResource("__files/altinn3/order-notification-notok.json")))
		);
	}

	@Configuration
	static class Config {
		@Bean
		RestClient restClient(RestClient.Builder builder) {
			return builder.build();
		}
	}
}
