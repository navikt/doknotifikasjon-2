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
import no.nav.doknotifikasjon.metrics.Metrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.ws.soap.SoapFaultException;

import javax.xml.bind.JAXBElement;
import java.util.List;

import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;
import static no.nav.doknotifikasjon.constants.RetryConstants.MAX_INT;
import static no.nav.doknotifikasjon.consumer.altinn.JAXBWrapper.ns;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_ALTIN_CONSUMER;

@Slf4j
@Service
public class AltinnVarselConsumer {

	private static final String SEND_TIL_ALTINN = "${SEND_TIL_ALTINN?: true }";

	@Value(SEND_TIL_ALTINN)
	private Boolean sendTilAltinn;

	private static final String DEFAULTNOTIFICATIONTYPE = "TokenTextOnly";
	private static final String TOKEN_VALUE = "TokenValue";
	private static final String IKKE_BESVAR_DENNE_NAV = "ikke-besvar-denne@nav.no";

	private final INotificationAgencyExternalBasic iNotificationAgencyExternalBasic;
	private final AltinnProps altinnProps;

	public AltinnVarselConsumer(INotificationAgencyExternalBasic iNotificationAgencyExternalBasic, AltinnProps altinnProps) {
		this.iNotificationAgencyExternalBasic = iNotificationAgencyExternalBasic;
		this.altinnProps = altinnProps;
	}

	@Metrics(value = DOK_ALTIN_CONSUMER, createErrorMetric = true, errorMetricInclude = AltinnTechnicalException.class)
	@Retryable(include = AltinnTechnicalException.class, maxAttempts = MAX_INT, backoff = @Backoff(delay = DELAY_LONG))
	public void sendVarsel(Kanal kanal, String kontaktInfo, String fnr, String tekst, String tittel) {
		//TODO: Fjern log
		log.error("Sendt til altinn er satt: "+sendTilAltinn);
		if (sendTilAltinn) {
			StandaloneNotificationBEList standaloneNotification = new StandaloneNotificationBEList().withStandaloneNotification(
					new StandaloneNotification()
							.withReporteeNumber(ns("ReporteeNumber", fnr))
							.withLanguageID(1044)
							.withNotificationType(ns("NotificationType", DEFAULTNOTIFICATIONTYPE))
							.withReceiverEndPoints(generateEndpoint(kanal, kontaktInfo))
							.withTextTokens(generateTextTokens(kanal, tekst, tittel))
							.withFromAddress(ns("FromAddress", IKKE_BESVAR_DENNE_NAV))
							.withUseServiceOwnerShortNameAsSenderOfSms(ns("UseServiceOwnerShortNameAsSenderOfSms", true)));
			try {
				iNotificationAgencyExternalBasic.sendStandaloneNotificationBasicV3(
						altinnProps.getUsername(),
						altinnProps.getPassword(),
						standaloneNotification
				);
			} catch (INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage e) {
				String errorMessage = e.getFaultInfo() != null ? e.getFaultInfo().getAltinnErrorMessage().toString() : e.getMessage();
				throw new AltinnFunctionalException(String.format("Feil av typen INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage ved kall mot Altinn. Feilmelding: %s", errorMessage), e);
			} catch (SoapFaultException e) {
				throw new AltinnFunctionalException(String.format("Feil av typen SoapFaultException ved kall mot Altinn. Feilmelding: %s", e.getMessage()), e);
			} catch (RuntimeException e) {
				throw new AltinnTechnicalException("Teknisk feil i kall mot Altinn.", e);
			} catch (Exception e) {
				throw new AltinnFunctionalException(String.format("Ukjent feil ved kall mot Altinn. Feilmelding: %s", e.getMessage()), e);
			}
		}else{
			log.info("Sender ikke melding til Altinn fordi flagg er satt");
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
		if (kanal == Kanal.SMS) {
			return ns("TextTokens",
					TextTokenSubstitutionBEList.class,
					new TextTokenSubstitutionBEList().withTextToken(List.of(
							new TextToken()
									.withTokenNum(0)
									.withTokenValue(ns(TOKEN_VALUE, tekst)),
							new TextToken()
									.withTokenNum(1)
									.withTokenValue(ns(TOKEN_VALUE, ""))
					)));
		}
		if (kanal == Kanal.EPOST) {
			return ns("TextTokens",
					TextTokenSubstitutionBEList.class,
					new TextTokenSubstitutionBEList().withTextToken(List.of(
							new TextToken()
									.withTokenNum(0)
									.withTokenValue(ns(TOKEN_VALUE, tittel)),
							new TextToken()
									.withTokenNum(1)
									.withTokenValue(ns(TOKEN_VALUE, tekst))
					)));
		}
		throw new AltinnFunctionalException("Funksjonell feil mot Altinn: Kanal er verken epost eller sms.");
	}

	private static TransportType kanalToTransportType(Kanal kanal) {
		if (Kanal.SMS == kanal) return TransportType.SMS;
		if (Kanal.EPOST == kanal) return TransportType.EMAIL;
		throw new AltinnFunctionalException("Kanal er verken SMS eller EMAIL, kanal=" + kanal);
	}
}
