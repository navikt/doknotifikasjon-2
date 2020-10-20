package no.nav.doknotifikasjon.consumer.altinn;

import lombok.extern.slf4j.Slf4j;
import no.altinn.springsoap.client.gen.*;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import javax.xml.bind.JAXBElement;
import java.util.Optional;


@Slf4j
public class AltinnConsumer extends WebServiceGatewaySupport {

    private final ObjectFactory objectFactory;
    private String username;
    private String password;

    private String SMS = "SMS";
    private String EMAIL = "EMAIL";

    public AltinnConsumer(String username, String password){
        this.objectFactory = new ObjectFactory();
        this.username = username;
        this.password = password;
    }

    public Optional<String> sendStandaloneNotificationV3(String kanal, String kontaktInfo, String tekst) {
        return sendStandaloneNotificationV3(kanal, kontaktInfo, tekst, "");
    }

    public Optional<String> sendStandaloneNotificationV3(String kanal, String kontaktInfo, String tekst, String tittel) {

        TransportType transportType = SMS.equals(kanal) ? TransportType.SMS : TransportType.EMAIL;

        SendStandaloneNotificationBasicV3 request = new SendStandaloneNotificationBasicV3();

        request.setStandaloneNotifications(createStandaloneNotificationBEList(transportType, kontaktInfo, tekst, tittel));
        request.setSystemUserName(username);
        request.setSystemPassword(password);

        try {
            getWebServiceTemplate().marshalSendAndReceive("http://localhost:8080/ws/countries", request,
                            new SoapActionCallback(
                                    "http://spring.io/guides/gs-producing-web-service/GetCountryRequest"));
            return Optional.empty();
        } catch (SoapFaultClientException exception) {
            log.error("SoapFaultClientException:", exception);
            return Optional.ofNullable(exception.getMessage()).map(String::strip);
        } catch (Exception exception) {
            log.error("Other exception:", exception);
            return Optional.ofNullable(exception.getMessage()).map(String::strip);
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

    public void setUsername(String username){
        this.username = username;
    }
    public void setPassword(String password){
        this.password = password;
    }

}
