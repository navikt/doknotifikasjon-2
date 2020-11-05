package no.nav.doknotifikasjon.consumer.altinn;

import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.repository.RepositoryConfig;
import no.nav.doknotifikasjon.repository.utils.ApplicationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest(
		classes = {ApplicationTestConfig.class, RepositoryConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("itestKafka")
public class AltinnConsumerTest {
/*
	private static final String KONTAKTINFO = "dummy adresse";
	private static final String TEKST = "dummy tekst";

	@Autowired


	private WebServiceTemplate webServiceTemplate = AltinnTestConfig.getWebServiceTemplateMock();

	@BeforeEach
	public void resetMock() {
		Mockito.reset(webServiceTemplate);
	}

	@Test
	public void shouldNotThrowWhenHappyPath() {
		when(webServiceTemplate.marshalSendAndReceive(any())).thenReturn(generateAltinnResponse(TransportType.SMS, KONTAKTINFO));

		altinnConsumer.sendStandaloneNotificationV3(Kanal.SMS, KONTAKTINFO, TEKST);

		verify(webServiceTemplate, times(1)).marshalSendAndReceive(any());
	}

	@Test
	public void shouldthrowFunctionalExceptionIfResponseIsEmpty() {

		when(webServiceTemplate.marshalSendAndReceive(any())).thenReturn(generateEmptyAltinnResponse());

		AltinnFunctionalException exception = assertThrows(
				AltinnFunctionalException.class,
				() -> {
					altinnConsumer.sendStandaloneNotificationV3(Kanal.SMS, KONTAKTINFO, TEKST);
				}
		);

		assertEquals("SendStandaloneNotificationBasicV3Response inneholder ikke notifikasjon som ble forsøkt sendt", exception.getMessage());
	}

	@Test
	public void shouldthrowFunctionalExceptionIfResponseIsNull() {

		when(webServiceTemplate.marshalSendAndReceive(any())).thenReturn(null);

		AltinnFunctionalException exception = assertThrows(
				AltinnFunctionalException.class,
				() -> {
					altinnConsumer.sendStandaloneNotificationV3(Kanal.SMS, KONTAKTINFO, TEKST);
				}
		);

		assertEquals("SendStandaloneNotificationBasicV3Response inneholder ikke notifikasjon som ble forsøkt sendt", exception.getMessage());
	}

	@Test
	public void shouldthrowFunctionalExceptionIfKanalIsNull() {
		AltinnFunctionalException exception = assertThrows(
				AltinnFunctionalException.class,
				() -> {
					altinnConsumer.sendStandaloneNotificationV3(null, KONTAKTINFO, TEKST);
				}
		);

		assertEquals("Kanal er verken SMS eller EMAIL, kanal=null", exception.getMessage());
	}


	@Test
	public void shouldthrowFunctionalExceptionIfResponseIsSoapFault() {

		when(webServiceTemplate.marshalSendAndReceive(any())).thenThrow(generateSoapFaultClientException("Dummy reason"));

		SoapFaultClientException exception = assertThrows(
				SoapFaultClientException.class,
				() -> {
					altinnConsumer.sendStandaloneNotificationV3(Kanal.SMS, KONTAKTINFO, TEKST);
				}
		);

		assertEquals("Dummy reason", exception.getMessage());
	}

	@Test
	public void shouldthrowTechnicalExceptionIfResponseIsUnknownException() {

		when(webServiceTemplate.marshalSendAndReceive(any())).thenThrow(new IllegalThreadStateException("Boom!"));

		AltinnTechnicalException exception = assertThrows(
				AltinnTechnicalException.class,
				() -> {
					altinnConsumer.sendStandaloneNotificationV3(Kanal.SMS, KONTAKTINFO, TEKST);
				}
		);

		assertEquals("sendStandaloneNotificationV3 ukjent feil, feilmelding=Boom!", exception.getMessage());
	}


 */
}