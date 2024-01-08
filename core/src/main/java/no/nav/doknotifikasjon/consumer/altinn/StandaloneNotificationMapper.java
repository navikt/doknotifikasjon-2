package no.nav.doknotifikasjon.consumer.altinn;

import jakarta.xml.bind.JAXBElement;
import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPoint;
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPointBEList;
import no.altinn.schemas.services.serviceengine.notification._2009._10.StandaloneNotification;
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextToken;
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextTokenSubstitutionBEList;
import no.altinn.schemas.services.serviceengine.standalonenotificationbe._2009._10.StandaloneNotificationBEList;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;

import static no.nav.doknotifikasjon.consumer.altinn.JAXBWrapper.ns;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;

public class StandaloneNotificationMapper {

	private static final String DEFAULTNOTIFICATIONTYPE = "TokenTextOnly";
	private static final String TOKEN_VALUE = "TokenValue";
	private static final String IKKE_BESVAR_DENNE_NAV = "ikke-besvar-denne@nav.no";

	public static StandaloneNotificationBEList map(Kanal kanal, String kontaktInfo, String fnr, String tekst, String tittel) {
		StandaloneNotification standaloneNotificationItem = new StandaloneNotification();
		standaloneNotificationItem.setReporteeNumber(ns("ReporteeNumber", fnr));
		standaloneNotificationItem.setLanguageID(1044);
		standaloneNotificationItem.setNotificationType(ns("NotificationType", DEFAULTNOTIFICATIONTYPE));
		standaloneNotificationItem.setReceiverEndPoints(generateEndpoint(kanal, kontaktInfo));
		standaloneNotificationItem.setTextTokens(generateTextTokens(kanal, tekst, tittel));
		standaloneNotificationItem.setFromAddress(ns("FromAddress", IKKE_BESVAR_DENNE_NAV));
		standaloneNotificationItem.setUseServiceOwnerShortNameAsSenderOfSms(ns("UseServiceOwnerShortNameAsSenderOfSms", true));

		StandaloneNotificationBEList standaloneNotification = new StandaloneNotificationBEList();
		standaloneNotification.getStandaloneNotification().add(standaloneNotificationItem);

		return standaloneNotification;
	}

	private static JAXBElement<ReceiverEndPointBEList> generateEndpoint(Kanal kanal, String kontaktInfo) {
		var receiverEndPoint = new ReceiverEndPoint();

		receiverEndPoint.setReceiverAddress(ns("ReceiverAddress", kontaktInfo));
		receiverEndPoint.setTransportType(ns("TransportType", TransportType.class, kanalToTransportType(kanal)));
		ReceiverEndPointBEList receiverEndPointBEList = new ReceiverEndPointBEList();
		receiverEndPointBEList.getReceiverEndPoint().add(receiverEndPoint);

		return ns("ReceiverEndPoints", ReceiverEndPointBEList.class, receiverEndPointBEList);
	}

	private static JAXBElement<TextTokenSubstitutionBEList> generateTextTokens(Kanal kanal, String tekst, String tittel) {
		if (kanal == SMS) {
			var textToken1 = new TextToken();
			textToken1.setTokenNum(0);
			textToken1.setTokenValue(ns(TOKEN_VALUE, tekst));
			var textToken2 = new TextToken();
			textToken2.setTokenNum(1);
			textToken2.setTokenValue(ns(TOKEN_VALUE, ""));

			var textTokenSubstitutionBEList = new TextTokenSubstitutionBEList();
			textTokenSubstitutionBEList.getTextToken().add(textToken1);
			textTokenSubstitutionBEList.getTextToken().add(textToken2);
			return ns("TextTokens",
					TextTokenSubstitutionBEList.class,
					textTokenSubstitutionBEList
			);
		}

		if (kanal == EPOST) {
			var textToken1 = new TextToken();
			textToken1.setTokenNum(0);
			textToken1.setTokenValue(ns(TOKEN_VALUE, tittel));
			var textToken2 = new TextToken();
			textToken2.setTokenNum(1);
			textToken2.setTokenValue(ns(TOKEN_VALUE, tekst));

			var textTokenSubstitutionBEList = new TextTokenSubstitutionBEList();
			textTokenSubstitutionBEList.getTextToken().add(textToken1);
			textTokenSubstitutionBEList.getTextToken().add(textToken2);
			return ns("TextTokens",
					TextTokenSubstitutionBEList.class,
					textTokenSubstitutionBEList);
		}

		throw new AltinnFunctionalException("Funksjonell feil mot Altinn: Kanal er verken epost eller sms.");
	}

	private static TransportType kanalToTransportType(Kanal kanal) {
		if (SMS == kanal) return TransportType.SMS;
		if (EPOST == kanal) return TransportType.EMAIL;

		throw new AltinnFunctionalException("Kanal er verken SMS eller EMAIL, kanal=" + kanal);
	}
}
