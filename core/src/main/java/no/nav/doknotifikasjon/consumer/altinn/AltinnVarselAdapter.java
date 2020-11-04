package no.nav.doknotifikasjon.consumer.altinn;

import lombok.extern.slf4j.Slf4j;
import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPoint;
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPointBEList;
import no.altinn.schemas.services.serviceengine.notification._2009._10.StandaloneNotification;
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextToken;
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextTokenSubstitutionBEList;
import no.altinn.schemas.services.serviceengine.standalonenotificationbe._2009._10.StandaloneNotificationBEList;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.List;

@Slf4j
@Service
public class AltinnVarselAdapter {

    private static final String DEFAULTNOTIFICATIONTYPE = "TokenTextOnly";


    private static final String NAMESPACE = "http://schemas.altinn.no/services/ServiceEngine/Notification/2009/10";

    private final INotificationAgencyExternalBasic iNotificationAgencyExternalBasic;
    private final AltinnProps altinnProps;

    public AltinnVarselAdapter(
            INotificationAgencyExternalBasic iNotificationAgencyExternalBasic,
            AltinnProps altinnProps
    ) {
        this.iNotificationAgencyExternalBasic = iNotificationAgencyExternalBasic;
        this.altinnProps = altinnProps;
    }

    private static JAXBElement<String> ns(String localpart, String value) {
        return new JAXBElement<>(new QName(NAMESPACE, localpart), String.class, value);
    }

    private static <T> JAXBElement<T> ns(String localpart, Class<T> clazz, T value) {
        return new JAXBElement<>(new QName(NAMESPACE, localpart), clazz, value);
    }

    private static JAXBElement<Boolean> ns(String localpart, Boolean value) {
        return new JAXBElement<>(new QName(NAMESPACE, localpart), Boolean.class, value);
    }

    public void sendVarsel(Kanal kanal, String kontaktInfo, String tekst, String tittel) {
        StandaloneNotificationBEList standaloneNotification = new StandaloneNotificationBEList().withStandaloneNotification(
                new StandaloneNotification()
                        .withLanguageID(1044)
                        .withNotificationType(ns("NotificationType", DEFAULTNOTIFICATIONTYPE))
                        .withReceiverEndPoints(generateEndpoint(kanal, kontaktInfo))
                        .withTextTokens(generateTextTokens(kanal, tekst, tittel))
                        .withUseServiceOwnerShortNameAsSenderOfSms(ns("UseServiceOwnerShortNameAsSenderOfSms", true)));
        try {
            iNotificationAgencyExternalBasic.sendStandaloneNotificationBasicV3(
                    altinnProps.getUsername(),
                    altinnProps.getPassword(),
                    standaloneNotification
            );
        } catch (AltinnFunctionalException e){
            throw new AltinnFunctionalException("functional exception", e);
        } catch (INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage | RuntimeException e) {
            throw new AltinnTechnicalException("Feil ved varsling gjennom Altinn", e);
        }
    }

    private JAXBElement<ReceiverEndPointBEList> generateEndpoint(Kanal kanal, String kontaktInfo) {
        return ns(
                "ReceiverEndPoints",
                ReceiverEndPointBEList.class,
                new ReceiverEndPointBEList()
                    .withReceiverEndPoint(
                            new ReceiverEndPoint()
                                    .withReceiverAddress(ns("ReceiverAddress", kontaktInfo))
                                    .withTransportType(ns("TransportType", TransportType.class, kanalToTransportType(kanal))))
        );
    }

    private JAXBElement<TextTokenSubstitutionBEList> generateTextTokens(Kanal kanal, String tekst, String tittel) {
        if(kanal == Kanal.SMS) {
            return ns("TextTokens",
                    TextTokenSubstitutionBEList.class,
                    new TextTokenSubstitutionBEList().withTextToken(List.of(
                            new TextToken()
                                    .withTokenNum(0)
                                    .withTokenValue(ns("TokenValue", tekst)),
                            new TextToken()
                                    .withTokenNum(1)
                                    .withTokenValue(ns("TokenValue", ""))
                    )));
        }
        if(kanal == Kanal.EPOST) {
            return ns("TextTokens",
                    TextTokenSubstitutionBEList.class,
                    new TextTokenSubstitutionBEList().withTextToken(List.of(
                            new TextToken()
                                    .withTokenNum(0)
                                    .withTokenValue(ns("TokenValue", tittel)),
                            new TextToken()
                                    .withTokenNum(1)
                                    .withTokenValue(ns("TokenValue", tekst))
                    )));
        }
        throw new AltinnFunctionalException("kanal er verken epost eller sms"); //TODO utfyllende
    }

    private static TransportType kanalToTransportType(Kanal kanal) {
        if (Kanal.SMS == kanal) return TransportType.SMS;
        if (Kanal.EPOST == kanal) return TransportType.EMAIL;
        throw new AltinnFunctionalException("Kanal er verken SMS eller EMAIL, kanal=" + kanal);
    }
}
