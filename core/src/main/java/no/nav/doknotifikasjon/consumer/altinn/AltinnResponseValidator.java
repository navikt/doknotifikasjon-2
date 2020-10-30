package no.nav.doknotifikasjon.consumer.altinn;

import no.altinn.springsoap.client.gen.EndPointResult;
import no.altinn.springsoap.client.gen.EndPointResultList;
import no.altinn.springsoap.client.gen.NotificationResult;
import no.altinn.springsoap.client.gen.SendNotificationResultList;
import no.altinn.springsoap.client.gen.SendStandaloneNotificationBasicV3Response;
import no.altinn.springsoap.client.gen.TransportType;
import no.nav.doknotifikasjon.kodeverk.Kanal;

import javax.xml.bind.JAXBElement;
import java.util.List;
import java.util.Optional;

public class AltinnResponseValidator {

	public static boolean isValidResponse(
			Kanal kanal,
			String kontaktInfo,
			SendStandaloneNotificationBasicV3Response response
	) {
		SendNotificationResultList sendNotificationResultList = Optional.ofNullable(response)
				.map(SendStandaloneNotificationBasicV3Response::getSendStandaloneNotificationBasicV3Result)
				.map(JAXBElement::getValue)
				.orElse(null);
		return isValidSendNotificationResultList(kanal, kontaktInfo, sendNotificationResultList);
	}

	private static boolean isValidSendNotificationResultList(Kanal kanal, String kontaktInfo, SendNotificationResultList sendNotificationResultList) {
		if (sendNotificationResultList == null) {
			return false;
		}

		List<NotificationResult> notificationResults = sendNotificationResultList.getNotificationResult();

		if (notificationResults == null) {
			return false;
		}

		return notificationResults.stream().anyMatch(notificationResult -> isValidSendNotificationResult(kanal, kontaktInfo, notificationResult));
	}

	private static boolean isValidSendNotificationResult(Kanal kanal, String kontaktInfo, NotificationResult notificationResult) {
		if (notificationResult == null) {
			return false;
		}

		List<EndPointResult> endPointResults = Optional.of(notificationResult)
				.map(NotificationResult::getEndPoints)
				.map(JAXBElement::getValue)
				.map(EndPointResultList::getEndPointResult)
				.orElse(null);

		if (endPointResults == null) {
			return false;
		}

		return endPointResults.stream().anyMatch(endPointResult -> isValidEndpointResult(kanal, kontaktInfo, endPointResult));
	}

	private static boolean isValidEndpointResult(Kanal kanal, String kontaktInfo, EndPointResult endPointResult) {
		if (endPointResult == null) {
			return false;
		}

		String endPointAddress = Optional.of(endPointResult)
				.map(EndPointResult::getReceiverAddress)
				.map(JAXBElement::getValue)
				.orElse(null);

		Kanal endpointKanal = transportTypeToKanal(endPointResult.getTransportType());

		if (endPointAddress == null || endpointKanal == null) {
			return false;
		}

		return kanal == endpointKanal && kontaktInfo.equals(endPointAddress);
	}

	private static Kanal transportTypeToKanal(TransportType transportType) {
		if (transportType == null) {
			return null;
		}

		if (transportType == TransportType.SMS) {
			return Kanal.SMS;
		}
		if (transportType == TransportType.EMAIL) {
			return Kanal.EPOST;
		}
		return null;
	}
}
