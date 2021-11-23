package no.nav.doknotifikasjon.consumer;

import no.altinn.schemas.services.serviceengine.notification._2015._06.SendNotificationResultList;
import no.altinn.services.common.fault._2009._10.AltinnFault;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.doknotifikasjon.consumer.altinn.AltinnVarselConsumer;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AltinnVarselConsumerTest {

	@Test
	void serviceShouldSendToAltinn() throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage, NoSuchFieldException {
		AltinnProps altinnProps = mock(AltinnProps.class);
		INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = mock(INotificationAgencyExternalBasic.class);
		AltinnVarselConsumer consumer = new AltinnVarselConsumer(true, iNotificationAgencyExternalBasic, altinnProps);
		doReturn(new SendNotificationResultList()).when(iNotificationAgencyExternalBasic).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
		doReturn("testname").when(altinnProps).getUsername();
		doReturn("testpassword").when(altinnProps).getPassword();
		consumer.sendVarsel(Kanal.EPOST, null, null, null, null);
		verify(iNotificationAgencyExternalBasic, times(1)).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
	}

	@Test
	void serviceShouldNotSendToAltinn() throws NoSuchFieldException, INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		AltinnProps altinnProps = mock(AltinnProps.class);
		INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = mock(INotificationAgencyExternalBasic.class);
		AltinnVarselConsumer consumer = new AltinnVarselConsumer(false, iNotificationAgencyExternalBasic, altinnProps);
		consumer.sendVarsel(Kanal.EPOST, null, null, null, null);
		verify(iNotificationAgencyExternalBasic, never()).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
	}

	@Test
	void shouldThrowUnwrappedMessageFromAltinnFaultWhenAltinnThrowsException() throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		AltinnProps altinnProps = mock(AltinnProps.class);
		INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = mock(INotificationAgencyExternalBasic.class);
		AltinnVarselConsumer consumer = new AltinnVarselConsumer(true, iNotificationAgencyExternalBasic, altinnProps);
		INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage altinnException = new INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage(
				"Feil i altinn",
				new AltinnFault()
						.withAltinnErrorMessage(constructJaxbElement("AltinnErrorMessage", "Ugyldig epostadresse angitt på et ReceiverEndPoint."))
						.withErrorGuid(constructJaxbElement("ErrorGuid", "fedcba"))
						.withErrorID(30010)
						.withUserGuid(constructJaxbElement("UserGuid", "abcdef"))
		);
		doThrow(altinnException)
				.when(iNotificationAgencyExternalBasic).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
		doReturn("testname").when(altinnProps).getUsername();
		doReturn("testpassword").when(altinnProps).getPassword();
		AltinnFunctionalException altinnFunctionalException = assertThrows(AltinnFunctionalException.class, () ->
				consumer.sendVarsel(Kanal.EPOST, null, null, null, null));
		assertEquals("Funksjonell feil fra Altinn. errorGuid=fedcba, userGuid=abcdef, errorId=30010, errorMessage=Ugyldig epostadresse angitt på et ReceiverEndPoint.", altinnFunctionalException.getMessage());
	}

	private JAXBElement<String> constructJaxbElement(String local, String value) {
		return new JAXBElement<>(new QName("http://www.altinn.no/services/common/fault/2009/10", local), String.class, value);
	}
}
