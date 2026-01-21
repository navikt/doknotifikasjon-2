package no.nav.doknotifikasjon.consumer;

import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPoint;
import no.altinn.schemas.services.serviceengine.notification._2009._10.StandaloneNotification;
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextToken;
import no.altinn.schemas.services.serviceengine.notification._2015._06.SendNotificationResultList;
import no.altinn.schemas.services.serviceengine.standalonenotificationbe._2009._10.StandaloneNotificationBEList;
import no.altinn.services.common.fault._2009._10.AltinnFault;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.doknotifikasjon.consumer.altinn.AltinnFunksjonellFeil;
import no.nav.doknotifikasjon.consumer.altinn2.Altinn2VarselConsumer;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import jakarta.xml.bind.JAXBElement;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.stream.Stream;

import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static org.assertj.core.api.Assertions.assertThat;
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

public class Altinn2VarselConsumerTest {

	private static final String FOEDSELSNUMMER = "01010156798";
	private static final String EPOST_TITTEL = "You've got mail!";
	private static final String EPOST_TEKST = "Dette er innholdet i en epost.";
	private static final String EPOST_ADRESSE = "epost@mottaker.no";
	private static final String TELEFONNUMMER = "+4712349876";
	private static final String SMS_TEKST = "Viktig melding fra NAV";

	static Stream<Arguments> serviceShouldSendToAltinn() {
		return Stream.of(
				Arguments.of(EPOST, EPOST_ADRESSE, FOEDSELSNUMMER, EPOST_TEKST, EPOST_TITTEL),
				Arguments.of(SMS, TELEFONNUMMER, FOEDSELSNUMMER, SMS_TEKST, null)
		);
	}
	@MethodSource
	@ParameterizedTest
	void serviceShouldSendToAltinn(Kanal kanal, String kontaktinformasjon, String fnr, String tekst, String tittel) throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		AltinnProps altinnProps = mock(AltinnProps.class);
		INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = mock(INotificationAgencyExternalBasic.class);
		Altinn2VarselConsumer consumer = new Altinn2VarselConsumer(true, iNotificationAgencyExternalBasic, altinnProps);
		doReturn(new SendNotificationResultList()).when(iNotificationAgencyExternalBasic).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
		doReturn("testname").when(altinnProps).getUsername();
		doReturn("testpassword").when(altinnProps).getPassword();

		consumer.sendVarsel(kanal, kontaktinformasjon, fnr, tekst, tittel);

		ArgumentCaptor<StandaloneNotificationBEList> xmlItem = ArgumentCaptor.forClass(StandaloneNotificationBEList.class);
		verify(iNotificationAgencyExternalBasic, times(1)).sendStandaloneNotificationBasicV3(anyString(), anyString(), xmlItem.capture());

		List<StandaloneNotification> standaloneNotificationList = xmlItem.getValue().getStandaloneNotification();
		assertNotificationContent(kanal, standaloneNotificationList);
	}

	private static void assertNotificationContent(Kanal kanal, List<StandaloneNotification> standaloneNotificationList) {
		assertThat(standaloneNotificationList).hasSize(1);

		StandaloneNotification standaloneNotification = standaloneNotificationList.get(0);
		assertThat(standaloneNotification.getReporteeNumber().getValue()).isEqualTo(FOEDSELSNUMMER);
		assertThat(standaloneNotification.getFromAddress().getValue()).isEqualTo("ikke-besvar-denne@nav.no");
		assertThat(standaloneNotification.getIsReservable()).isNull();
		assertThat(standaloneNotification.getLanguageID().intValue()).isEqualTo(1044);
		assertThat(standaloneNotification.getNotificationType().getValue()).isEqualTo("TokenTextOnly");
		assertThat(standaloneNotification.getUseServiceOwnerShortNameAsSenderOfSms().getValue()).isEqualTo(true);

		assertThat(standaloneNotification.getRoles()).isNull();
		assertThat(standaloneNotification.getService()).isNull();

		List<ReceiverEndPoint> endPointList = standaloneNotification.getReceiverEndPoints().getValue().getReceiverEndPoint();
		assertThat(endPointList).hasSize(1);
		assertThat(standaloneNotification.getShipmentDateTime()).isNull();

		if (kanal == EPOST) {
			ReceiverEndPoint receiverEndpoint = endPointList.get(0);
			assertThat(receiverEndpoint.getReceiverAddress().getValue()).isEqualTo(EPOST_ADRESSE);
			assertThat(receiverEndpoint.getTransportType().getValue()).isEqualTo(TransportType.EMAIL);

			List<TextToken> textTokenList = standaloneNotification.getTextTokens().getValue().getTextToken();
			assertThat(textTokenList).hasSize(2);

			assertThat(textTokenList.get(0).getTokenValue().getValue()).isEqualTo(EPOST_TITTEL);
			assertThat(textTokenList.get(1).getTokenValue().getValue()).isEqualTo(EPOST_TEKST);
		} else if (kanal == SMS) {
			ReceiverEndPoint receiverEndpoint = endPointList.get(0);
			assertThat(receiverEndpoint.getReceiverAddress().getValue()).isEqualTo(TELEFONNUMMER);
			assertThat(receiverEndpoint.getTransportType().getValue()).isEqualTo(TransportType.SMS);

			List<TextToken> textTokenList = standaloneNotification.getTextTokens().getValue().getTextToken();
			assertThat(textTokenList).hasSize(2);

			assertThat(textTokenList.get(0).getTokenValue().getValue()).isEqualTo(SMS_TEKST);
			assertThat(textTokenList.get(1).getTokenValue().getValue()).isEmpty();

		} else {
			throw new AssertionError("kanal må være enten SMS eller EPOST, var " + kanal);
		}
	}

	@Test
	void serviceShouldNotSendToAltinn() throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		AltinnProps altinnProps = mock(AltinnProps.class);
		INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = mock(INotificationAgencyExternalBasic.class);
		Altinn2VarselConsumer consumer = new Altinn2VarselConsumer(false, iNotificationAgencyExternalBasic, altinnProps);

		consumer.sendVarsel(EPOST, null, null, null, null);

		verify(iNotificationAgencyExternalBasic, never()).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
	}

	@ParameterizedTest
	@EnumSource(AltinnFunksjonellFeil.class)
	void shouldThrowUnwrappedMessageFromAltinnFaultWhenAltinnThrowsFunksjonellException(AltinnFunksjonellFeil feil) throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		AltinnProps altinnProps = mock(AltinnProps.class);
		INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = mock(INotificationAgencyExternalBasic.class);
		Altinn2VarselConsumer consumer = new Altinn2VarselConsumer(true, iNotificationAgencyExternalBasic, altinnProps);

		var altinnFault = new AltinnFault();
		altinnFault.setAltinnErrorMessage(constructJaxbElement("AltinnErrorMessage", feil.beskrivelse));
		altinnFault.setErrorGuid(constructJaxbElement("ErrorGuid", "fedcba"));
		altinnFault.setErrorID(feil.feilkode);
		altinnFault.setUserGuid(constructJaxbElement("UserGuid", "abcdef"));

		INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage altinnException = new INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage(
				"Feil i altinn", altinnFault
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
		Altinn2VarselConsumer consumer = new Altinn2VarselConsumer(true, iNotificationAgencyExternalBasic, altinnProps);
		AltinnFault altinnFault = new AltinnFault();
		altinnFault.setAltinnErrorMessage(constructJaxbElement("AltinnErrorMessage", beskrivelse));
		altinnFault.setErrorGuid(constructJaxbElement("ErrorGuid", "fedcba"));
		altinnFault.setErrorID(feilkode);
		altinnFault.setUserGuid(constructJaxbElement("UserGuid", "abcdef"));
		INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage altinnException = new INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage(
				"Feil i altinn", altinnFault
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
