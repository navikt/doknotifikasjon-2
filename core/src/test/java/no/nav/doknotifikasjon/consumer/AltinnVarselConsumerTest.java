package no.nav.doknotifikasjon.consumer;

import no.altinn.schemas.services.serviceengine.notification._2015._06.SendNotificationResultList;
import no.altinn.services.common.fault._2009._10.AltinnFault;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.doknotifikasjon.consumer.altinn.AltinnFunksjonellFeil;
import no.nav.doknotifikasjon.consumer.altinn.AltinnVarselConsumer;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.stream.Stream;

import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
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
import static org.mockito.Mockito.when;

public class AltinnVarselConsumerTest {

	@Test
	void serviceShouldSendToAltinn() throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		AltinnProps altinnProps = mock(AltinnProps.class);
		INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = mock(INotificationAgencyExternalBasic.class);
		AltinnVarselConsumer consumer = new AltinnVarselConsumer(true, iNotificationAgencyExternalBasic, altinnProps);
		doReturn(new SendNotificationResultList()).when(iNotificationAgencyExternalBasic).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
		doReturn("testname").when(altinnProps).getUsername();
		doReturn("testpassword").when(altinnProps).getPassword();

		consumer.sendVarsel(EPOST, null, null, null, null);

		verify(iNotificationAgencyExternalBasic, times(1)).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
	}

	@Test
	void serviceShouldNotSendToAltinn() throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		AltinnProps altinnProps = mock(AltinnProps.class);
		INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = mock(INotificationAgencyExternalBasic.class);
		AltinnVarselConsumer consumer = new AltinnVarselConsumer(false, iNotificationAgencyExternalBasic, altinnProps);

		consumer.sendVarsel(EPOST, null, null, null, null);

		verify(iNotificationAgencyExternalBasic, never()).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
	}

	@ParameterizedTest
	@EnumSource(AltinnFunksjonellFeil.class)
	void shouldThrowUnwrappedMessageFromAltinnFaultWhenAltinnThrowsFunksjonellException(AltinnFunksjonellFeil feil) throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		AltinnProps altinnProps = mock(AltinnProps.class);
		INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = mock(INotificationAgencyExternalBasic.class);
		AltinnVarselConsumer consumer = new AltinnVarselConsumer(true, iNotificationAgencyExternalBasic, altinnProps);
		INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage altinnException = new INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage(
				"Feil i altinn",
				new AltinnFault()
						.withAltinnErrorMessage(constructJaxbElement("AltinnErrorMessage", feil.beskrivelse))
						.withErrorGuid(constructJaxbElement("ErrorGuid", "fedcba"))
						.withErrorID(feil.feilkode)
						.withUserGuid(constructJaxbElement("UserGuid", "abcdef"))
		);
		doThrow(altinnException)
				.when(iNotificationAgencyExternalBasic).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
		doReturn("testname").when(altinnProps).getUsername();
		doReturn("testpassword").when(altinnProps).getPassword();

		AltinnFunctionalException altinnFunctionalException = assertThrows(AltinnFunctionalException.class, () ->
				consumer.sendVarsel(EPOST, null, null, null, null));

		String expectedMessage = String.format("Funksjonell feil i kall mot Altinn. errorGuid=fedcba, userGuid=abcdef, errorId=%s, errorMessage=%s", feil.feilkode, feil.beskrivelse);
		assertEquals(expectedMessage, altinnFunctionalException.getMessage());
	}

	@ParameterizedTest
	@MethodSource("provideTechnicalExceptions")
	void shouldThrowUnwrappedMessageFromAltinnFaultWhenAltinnThrowsTechnicalException(Integer feilkode, String beskrivelse) throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		AltinnProps altinnProps = mock(AltinnProps.class);
		INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = mock(INotificationAgencyExternalBasic.class);
		AltinnVarselConsumer consumer = new AltinnVarselConsumer(true, iNotificationAgencyExternalBasic, altinnProps);
		INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage altinnException = new INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage(
				"Feil i altinn",
				new AltinnFault()
						.withAltinnErrorMessage(constructJaxbElement("AltinnErrorMessage", beskrivelse))
						.withErrorGuid(constructJaxbElement("ErrorGuid", "fedcba"))
						.withErrorID(feilkode)
						.withUserGuid(constructJaxbElement("UserGuid", "abcdef"))
		);

		when(iNotificationAgencyExternalBasic.sendStandaloneNotificationBasicV3(anyString(), anyString(), any())).thenThrow(altinnException);
		doReturn("testname").when(altinnProps).getUsername();
		doReturn("testpassword").when(altinnProps).getPassword();

		AltinnTechnicalException altinnTechnicalException = assertThrows(AltinnTechnicalException.class, () -> consumer.sendVarsel(EPOST, null, null, null, null));

		String expectedMessage = String.format("Teknisk feil i kall mot Altinn. errorGuid=fedcba, userGuid=abcdef, errorId=%s, errorMessage=%s", feilkode, beskrivelse);
		assertEquals(expectedMessage, altinnTechnicalException.getMessage());
	}

	private static Stream<Arguments> provideTechnicalExceptions() {
		return Stream.of(
				Arguments.of(0, "Your request suffered from a non-functional error. If this persist, please report it to the system administrator."),
				Arguments.of(44, "An exception happened when trying to authenticate the system"),
				Arguments.of(null, "Do retry if null")
		);
	}

	private JAXBElement<String> constructJaxbElement(String local, String value) {
		return new JAXBElement<>(new QName("http://www.altinn.no/services/common/fault/2009/10", local), String.class, value);
	}
}
