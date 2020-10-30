package no.nav.doknotifikasjon.consumer.altinn;

import no.altinn.springsoap.client.gen.ObjectFactory;
import no.altinn.springsoap.client.gen.ReceiverEndPoint;
import no.altinn.springsoap.client.gen.ReceiverEndPointBEList;
import no.altinn.springsoap.client.gen.SendStandaloneNotificationBasicV3;
import no.altinn.springsoap.client.gen.StandaloneNotification;
import no.altinn.springsoap.client.gen.StandaloneNotificationBEList;
import no.altinn.springsoap.client.gen.TextToken;
import no.altinn.springsoap.client.gen.TextTokenSubstitutionBEList;
import no.altinn.springsoap.client.gen.TransportType;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;

import javax.xml.bind.JAXBElement;

public class AltinnRequestMapper {

	private static final ObjectFactory objectFactory = new ObjectFactory();

	private static final String DEFAULTNOTIFICATIONEPOSTADDRESSE = "ikke-besvar-denne@nav.no";
	private static final String DEFAULTNOTIFICATIONTYPE = "TokenTextOnly";

	public static SendStandaloneNotificationBasicV3 createRequest(
			Kanal kanal,
			String kontaktInfo,
			String tekst,
			String tittel,
			String password,
			String username
	) {
		TransportType transportType = kanalToTransportType(kanal);

		SendStandaloneNotificationBasicV3 request = new SendStandaloneNotificationBasicV3();

		request.setStandaloneNotifications(createStandaloneNotificationBEList(transportType, kontaktInfo, tekst, tittel));
		request.setSystemUserName(username);
		request.setSystemPassword(password);

		return request;
	}

	private static StandaloneNotificationBEList createStandaloneNotificationBEList(TransportType transportType, String kontaktInfo, String tekst, String tittel) {
		StandaloneNotification standaloneNotification = objectFactory.createStandaloneNotification();

		standaloneNotification.setNotificationType(objectFactory.createStandaloneNotificationNotificationType(DEFAULTNOTIFICATIONTYPE));

		standaloneNotification.setTextTokens(createTextTokenSubstitutionBEList(transportType, tekst, tittel));
		standaloneNotification.setReceiverEndPoints(createReceiverEndPointBEList(transportType, kontaktInfo));

		if (transportType == TransportType.EMAIL) {
			standaloneNotification.setFromAddress(objectFactory.createStandaloneNotificationFromAddress(DEFAULTNOTIFICATIONEPOSTADDRESSE));
		}
		if (transportType == TransportType.SMS) {
			standaloneNotification.setUseServiceOwnerShortNameAsSenderOfSms(
					objectFactory.createStandaloneNotificationUseServiceOwnerShortNameAsSenderOfSms(true)
			);
		}

		StandaloneNotificationBEList standaloneNotificationBEList = objectFactory.createStandaloneNotificationBEList();
		standaloneNotificationBEList.getStandaloneNotification().add(standaloneNotification);

		return standaloneNotificationBEList;
	}

	private static JAXBElement<TextTokenSubstitutionBEList> createTextTokenSubstitutionBEList(TransportType transportType, String tekst, String tittel) {
		TextTokenSubstitutionBEList textTokenSubstitutionBEList = objectFactory.createTextTokenSubstitutionBEList();
		TextToken textToken1 = objectFactory.createTextToken();
		TextToken textToken2 = objectFactory.createTextToken();


		if (transportType == TransportType.SMS) {

			/*
			 * Kevin Sillerud 08/04/2019 20:55 Send a empty string for SMS titles
			 * Altinn seems to be saving messages sent after 17:00ish in a database,
			 * however they do not expect the TextToken field to be null,
			 * so SMS notifications sent after 17:00 would fail and result in an error while persisting
			 */

			textToken1.setTokenNum(0);
			textToken1.setTokenValue(objectFactory.createTextTokenTokenValue(tekst));

			textToken2.setTokenNum(1);
			textToken2.setTokenValue(objectFactory.createTextTokenTokenValue(""));
		}

		if (transportType == TransportType.EMAIL) {
			textToken1.setTokenNum(0);
			textToken1.setTokenValue(objectFactory.createTextTokenTokenValue(tittel));

			textToken2.setTokenNum(1);
			textToken2.setTokenValue(objectFactory.createTextTokenTokenValue(tekst));
		}

		textTokenSubstitutionBEList.getTextToken().add(textToken1);
		textTokenSubstitutionBEList.getTextToken().add(textToken2);

		return objectFactory.createTextTokenSubstitutionBEList(textTokenSubstitutionBEList);
	}

	private static JAXBElement<ReceiverEndPointBEList> createReceiverEndPointBEList(TransportType transportType, String kontaktInfo) {

		ReceiverEndPoint receiverEndPoint = objectFactory.createReceiverEndPoint();

		receiverEndPoint.setReceiverAddress(objectFactory.createReceiverEndPointReceiverAddress(kontaktInfo));
		receiverEndPoint.setTransportType(objectFactory.createReceiverEndPointTransportType(transportType));

		ReceiverEndPointBEList receiverEndPointBEList = objectFactory.createReceiverEndPointBEList();
		receiverEndPointBEList.getReceiverEndPoint().add(receiverEndPoint);

		return objectFactory.createReceiverEndPointBEList(receiverEndPointBEList);
	}

	private static TransportType kanalToTransportType(Kanal kanal) {
		if (Kanal.SMS == kanal) return TransportType.SMS;
		if (Kanal.EPOST == kanal) return TransportType.EMAIL;
		throw new AltinnFunctionalException("Kanal er verken SMS eller EMAIL, kanal=" + kanal);
	}
}
