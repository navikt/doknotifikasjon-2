package no.nav.doknotifikasjon.consumer.altinn;

import lombok.extern.slf4j.Slf4j;
import no.altinn.springsoap.client.gen.EndPointResult;
import no.altinn.springsoap.client.gen.EndPointResultList;
import no.altinn.springsoap.client.gen.NotificationResult;
import no.altinn.springsoap.client.gen.ObjectFactory;
import no.altinn.springsoap.client.gen.SendNotificationResultList;
import no.altinn.springsoap.client.gen.SendStandaloneNotificationBasicV3Response;
import no.altinn.springsoap.client.gen.TransportType;
import no.altinn.springsoap.client.gen.SendStandaloneNotificationBasicV3;
import no.altinn.springsoap.client.gen.StandaloneNotificationBEList;
import no.altinn.springsoap.client.gen.StandaloneNotification;
import no.altinn.springsoap.client.gen.TextTokenSubstitutionBEList;
import no.altinn.springsoap.client.gen.TextToken;
import no.altinn.springsoap.client.gen.ReceiverEndPoint;
import no.altinn.springsoap.client.gen.ReceiverEndPointBEList;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.SoapFaultClientException;

import javax.xml.bind.JAXBElement;
import java.util.List;
import java.util.Optional;

@Slf4j
public class AltinnConsumer extends WebServiceGatewaySupport {

    private final ObjectFactory objectFactory;
    private String username;
    private String password;

    public AltinnConsumer(String username, String password){
        this.objectFactory = new ObjectFactory();
        this.username = username;
        this.password = password;
    }

    public void sendStandaloneNotificationV3(Kanal kanal, String kontaktInfo, String tekst) {
        sendStandaloneNotificationV3(kanal, kontaktInfo, tekst, "");
    }

    public void sendStandaloneNotificationV3(Kanal kanal, String kontaktInfo, String tekst, String tittel) {

        TransportType transportType = Kanal.SMS == kanal ? TransportType.SMS : TransportType.EMAIL;

        SendStandaloneNotificationBasicV3 request = new SendStandaloneNotificationBasicV3();

        request.setStandaloneNotifications(createStandaloneNotificationBEList(transportType, kontaktInfo, tekst, tittel));
        request.setSystemUserName(username);
        request.setSystemPassword(password);

        try {
            SendStandaloneNotificationBasicV3Response response = (SendStandaloneNotificationBasicV3Response) getWebServiceTemplate().marshalSendAndReceive(request);


            SendNotificationResultList sendNotificationResultList = Optional.ofNullable(response)
                    .map(SendStandaloneNotificationBasicV3Response::getSendStandaloneNotificationBasicV3Result)
                    .map(JAXBElement::getValue).orElse(null);

            if (!validateSendNotificationResultList(kanal, kontaktInfo, sendNotificationResultList)) {
                throw new AltinnFunctionalException("Respons inneholder ikke notifikasjon");
            }

        } catch (SoapFaultClientException exception) {
            log.error(
                    "sendStandaloneNotificationV3 Det oppstod en feil ved sending av request: faultCode=${} faultStringOrReason=${}",
                    exception.getFaultCode(),
                    exception.getFaultStringOrReason(),
                    exception
            );
            throw exception;
        } catch (AltinnFunctionalException exception){
            log.error("sendStandaloneNotificationV3 respons inneholder ikke notifikasjon", exception);
            throw exception;
        } catch (Exception exception) {
            log.error("sendStandaloneNotificationV3 ukjent feil", exception);
            throw exception;
        }
    }

    private StandaloneNotificationBEList createStandaloneNotificationBEList (TransportType transportType, String kontaktInfo, String tekst, String tittel){
        StandaloneNotification standaloneNotification = objectFactory.createStandaloneNotification();

        standaloneNotification.setNotificationType(objectFactory.createStandaloneNotificationNotificationType("TokenTextOnly"));

        standaloneNotification.setTextTokens(createTextTokenSubstitutionBEList(transportType, tekst, tittel));
        standaloneNotification.setReceiverEndPoints(createReceiverEndPointBEList(transportType, kontaktInfo));

        if(transportType == TransportType.EMAIL){
            standaloneNotification.setFromAddress(objectFactory.createStandaloneNotificationFromAddress("ikke-besvar-denne@nav.no"));
        }
        if(transportType == TransportType.SMS) {
            standaloneNotification.setUseServiceOwnerShortNameAsSenderOfSms(
                    objectFactory.createStandaloneNotificationUseServiceOwnerShortNameAsSenderOfSms(true)
            );
        }

        StandaloneNotificationBEList standaloneNotificationBEList = objectFactory.createStandaloneNotificationBEList();
        standaloneNotificationBEList.getStandaloneNotification().add(standaloneNotification);

        return standaloneNotificationBEList;
    }

    private JAXBElement<TextTokenSubstitutionBEList> createTextTokenSubstitutionBEList(TransportType transportType, String tekst, String tittel) {
        TextTokenSubstitutionBEList textTokenSubstitutionBEList = objectFactory.createTextTokenSubstitutionBEList();
        TextToken textToken1 = objectFactory.createTextToken();
        TextToken textToken2 = objectFactory.createTextToken();


        if(transportType == TransportType.SMS){

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

        if(transportType == TransportType.EMAIL) {
            textToken1.setTokenNum(0);
            textToken1.setTokenValue(objectFactory.createTextTokenTokenValue(tittel));

            textToken2.setTokenNum(1);
            textToken2.setTokenValue(objectFactory.createTextTokenTokenValue(tekst));
        }

        textTokenSubstitutionBEList.getTextToken().add(textToken1);
        textTokenSubstitutionBEList.getTextToken().add(textToken2);

        return objectFactory.createTextTokenSubstitutionBEList(textTokenSubstitutionBEList);
    }

    private JAXBElement<ReceiverEndPointBEList> createReceiverEndPointBEList(TransportType transportType, String kontaktInfo) {

        ReceiverEndPoint receiverEndPoint = objectFactory.createReceiverEndPoint();

        receiverEndPoint.setReceiverAddress(objectFactory.createReceiverEndPointReceiverAddress(kontaktInfo));
        receiverEndPoint.setTransportType(objectFactory.createReceiverEndPointTransportType(transportType));

        ReceiverEndPointBEList receiverEndPointBEList = objectFactory.createReceiverEndPointBEList();
        receiverEndPointBEList.getReceiverEndPoint().add(receiverEndPoint);

        return objectFactory.createReceiverEndPointBEList(receiverEndPointBEList);
    }

    private boolean validateSendNotificationResultList (Kanal kanal, String kontaktInfo, SendNotificationResultList sendNotificationResultList) {
        if(sendNotificationResultList == null){ return false; }

        List<NotificationResult> notificationResults = sendNotificationResultList.getNotificationResult();

        if(notificationResults == null) { return false; }

        return notificationResults.stream().anyMatch(notificationResult -> validateSendNotificationResult(kanal, kontaktInfo, notificationResult));
    }

    private boolean validateSendNotificationResult (Kanal kanal, String kontaktInfo, NotificationResult notificationResult) {
        if(notificationResult == null) { return false; }

        List<EndPointResult> endPointResults = Optional.of(notificationResult)
                .map(NotificationResult::getEndPoints)
                .map(JAXBElement::getValue)
                .map(EndPointResultList::getEndPointResult).orElse(null);

        if(endPointResults == null) { return false; }

        return endPointResults.stream().anyMatch(endPointResult -> validateEndpointResult(kanal, kontaktInfo, endPointResult));
    }

    private boolean validateEndpointResult (Kanal kanal, String kontaktInfo, EndPointResult endPointResult){
        if(endPointResult == null){ return false; }

        String endPointAddress = Optional.of(endPointResult)
                .map(EndPointResult::getReceiverAddress)
                .map(JAXBElement::getValue).orElse(null);

        Kanal endpointKanal = transportTypeToKanal(endPointResult.getTransportType());

        if(endPointAddress == null || endpointKanal == null){ return false; }

        return kanal == endpointKanal && kontaktInfo.equals(endPointAddress);
    }

    private Kanal transportTypeToKanal(TransportType transportType){
        if(transportType == null) { return null; }

        if(transportType == TransportType.SMS){
            return Kanal.SMS;
        }
        if(transportType == TransportType.EMAIL){
            return Kanal.EPOST;
        }
        return null;
    }
}
