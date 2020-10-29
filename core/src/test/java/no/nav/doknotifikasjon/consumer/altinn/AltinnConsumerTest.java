package no.nav.doknotifikasjon.consumer.altinn;

import no.altinn.springsoap.client.gen.TransportType;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.repository.RepositoryConfig;
import no.nav.doknotifikasjon.repository.utils.ApplicationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.InputStreamSource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.mime.Attachment;
import org.springframework.ws.mime.AttachmentException;
import org.springframework.ws.soap.SoapBody;
import org.springframework.ws.soap.SoapBodyException;
import org.springframework.ws.soap.SoapEnvelope;
import org.springframework.ws.soap.SoapEnvelopeException;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapHeaderException;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.w3c.dom.Document;


import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import static no.nav.doknotifikasjon.consumer.altinn.AltinResponseFactory.generateAltinnResponse;
import static no.nav.doknotifikasjon.consumer.altinn.AltinResponseFactory.generateEmptyAltinnResponse;
import static no.nav.doknotifikasjon.consumer.altinn.AltinResponseFactory.generateSoapFaultClientException;
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

	private static final String KONTAKTINFO = "dummy adresse";
	private static final String TEKST = "dummy tekst";

    @Autowired
    AltinnConsumer altinnConsumer;

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
				()->{ altinnConsumer.sendStandaloneNotificationV3(Kanal.SMS, KONTAKTINFO, TEKST); }
		);

		assertEquals("Respons inneholder ikke notifikasjon", exception.getMessage());
	}

	@Test
	public void shouldthrowFunctionalExceptionIfResponseIsNull() {

		when(webServiceTemplate.marshalSendAndReceive(any())).thenReturn(null);

		AltinnFunctionalException exception = assertThrows(
				AltinnFunctionalException.class,
				()->{ altinnConsumer.sendStandaloneNotificationV3(Kanal.SMS, KONTAKTINFO, TEKST); }
		);

		assertEquals("Respons inneholder ikke notifikasjon", exception.getMessage());
	}

	@Test
	public void shouldthrowFunctionalExceptionIfKanalIsNull() {
		AltinnFunctionalException exception = assertThrows(
				AltinnFunctionalException.class,
				()->{ altinnConsumer.sendStandaloneNotificationV3(null, KONTAKTINFO, TEKST); }
		);

		assertEquals("Kanal er verken SMS eller EMAIL, kanal=null", exception.getMessage());
	}


	@Test
	public void shouldthrowFunctionalExceptionIfResponseIsSoapFault() {

		when(webServiceTemplate.marshalSendAndReceive(any())).thenThrow(generateSoapFaultClientException("Dummy reason"));

		SoapFaultClientException exception = assertThrows(
				SoapFaultClientException.class,
				()->{ altinnConsumer.sendStandaloneNotificationV3(Kanal.SMS, KONTAKTINFO, TEKST); }
		);

		assertEquals("Dummy reason", exception.getMessage());
	}

	@Test
	public void shouldthrowTechnicalExceptionIfResponseIsUnknownException() {

		when(webServiceTemplate.marshalSendAndReceive(any())).thenThrow(new IllegalThreadStateException("Boom!"));

		AltinnTechnicalException exception = assertThrows(
				AltinnTechnicalException.class,
				()->{ altinnConsumer.sendStandaloneNotificationV3(Kanal.SMS, KONTAKTINFO, TEKST); }
		);

		assertEquals("sendStandaloneNotificationV3 ukjent feil, feilmelding=Boom!", exception.getMessage());
	}



}