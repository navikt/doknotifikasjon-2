package no.nav.doknotifikasjon.consumer.altinn;

import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPoint;
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPointBEList;
import no.altinn.schemas.services.serviceengine.notification._2009._10.StandaloneNotification;
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextToken;
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextTokenSubstitutionBEList;
import no.altinn.schemas.services.serviceengine.standalonenotificationbe._2009._10.StandaloneNotificationBEList;
import no.altinn.services.common.fault._2009._10.AltinnFault;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.metrics.Metrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.ws.soap.SoapFaultException;

import static java.lang.String.format;
import static no.nav.doknotifikasjon.consumer.altinn.AltinnFunksjonellFeil.erFunksjonellFeil;
import static no.nav.doknotifikasjon.consumer.altinn.JAXBWrapper.ns;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_ALTIN_CONSUMER;

@Slf4j
@Service
public class AltinnVarselConsumer {

	private static final String SEND_TIL_ALTINN = "${SEND_TIL_ALTINN}";
	private final Boolean sendTilAltinn;

	private static final String DEFAULTNOTIFICATIONTYPE = "TokenTextOnly";
	private static final String TOKEN_VALUE = "TokenValue";
	private static final String IKKE_BESVAR_DENNE_NAV = "ikke-besvar-denne@nav.no";

	private final INotificationAgencyExternalBasic iNotificationAgencyExternalBasic;
	private final AltinnProps altinnProps;

	public AltinnVarselConsumer(@Value(SEND_TIL_ALTINN) Boolean sendTilAltinn, INotificationAgencyExternalBasic iNotificationAgencyExternalBasic, AltinnProps altinnProps) {
		this.iNotificationAgencyExternalBasic = iNotificationAgencyExternalBasic;
		this.altinnProps = altinnProps;
		this.sendTilAltinn = sendTilAltinn;
	}

	@Metrics(value = DOK_ALTIN_CONSUMER, createErrorMetric = true, errorMetricInclude = AltinnTechnicalException.class)
	@Retryable(
			retryFor = AltinnTechnicalException.class,
			maxAttemptsExpression = "${retry.attempts:10}",
			backoff = @Backoff(delayExpression = "${retry.delay:1000}", multiplier = 2, maxDelay = 60_000L)
	)
	public void sendVarsel(Kanal kanal, String kontaktInfo, String fnr, String tekst, String tittel) {
		if (!sendTilAltinn) {
			log.info("Sender ikke melding til Altinn. flagget sendTilAltinn=false");
			return;
		}

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

		try {
			iNotificationAgencyExternalBasic.sendStandaloneNotificationBasicV3(
					altinnProps.getUsername(),
					altinnProps.getPassword(),
					standaloneNotification
			);
		} catch (INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage e) {
			final String altinnErrorMessage = constructAltinnErrorMessage(e);

			Integer feilkode = getFeilkode(e);
			if (erFunksjonellFeil(feilkode)) {
				throw new AltinnFunctionalException(format("Funksjonell feil i kall mot Altinn. %s", altinnErrorMessage), e);
			} else {
				if (erUhandterbarTekniskFeil(e)) {
					throw new AltinnFunctionalException(format("Uhandterbar teknisk feil feil i kall mot Altinn. Håndteres som funksjonell feil. %s", altinnErrorMessage), e);
				}
				throw new AltinnTechnicalException(format("Teknisk feil i kall mot Altinn. %s", altinnErrorMessage), e);
			}
		} catch (SoapFaultException e) {
			throw new AltinnFunctionalException(format("Funksjonell feil i kall mot Altinn. Feilmelding: %s", e.getMessage()), e);
		} catch (RuntimeException e) {
			throw new AltinnTechnicalException("Ukjent teknisk feil i kall mot Altinn.", e);
		} catch (Exception e) {
			throw new AltinnTechnicalException("Ukjent teknisk feil ved kall mot Altinn.", e);
		}
	}

	private boolean erUhandterbarTekniskFeil(INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage e) {
		AltinnFault faultInfo = e.getFaultInfo();

		if (faultInfo != null) {
			log.error("Utvidet teknisk feil info errorGuid={}, altinnLocalizedErrorMessage={}, altinnExtendedErrorMessage={}",
					unwrap(faultInfo.getErrorGuid()), unwrap(faultInfo.getAltinnLocalizedErrorMessage()), unwrap(faultInfo.getAltinnExtendedErrorMessage()));
			return unwrap(faultInfo.getAltinnLocalizedErrorMessage()).contains("Object reference not set to an instance of an object");
		}

		return false;
	}

	@Recover
	public void altinnTechnicalExceptionRecovery(AltinnTechnicalException e) {
		log.warn("Teknisk feil for sending av sms/epost til Altinn - maks. antall forsøk brukt");

		throw e;
	}

	// Catch-all for alle andre exceptions - hvis ikke blir ExhaustedRetryException kastet med meldingen 'Cannot locate recovery method'
	@Recover
	public void otherExceptionsRecovery(RuntimeException e) {
		throw e;
	}

	private JAXBElement<ReceiverEndPointBEList> generateEndpoint(Kanal kanal, String kontaktInfo) {
		var receiverEndPoint = new ReceiverEndPoint();

		receiverEndPoint.setReceiverAddress(ns("ReceiverAddress", kontaktInfo));
		receiverEndPoint.setTransportType(ns("TransportType", TransportType.class, kanalToTransportType(kanal)));
		ReceiverEndPointBEList receiverEndPointBEList = new ReceiverEndPointBEList();
		receiverEndPointBEList.getReceiverEndPoint().add(receiverEndPoint);

		return ns("ReceiverEndPoints", ReceiverEndPointBEList.class, receiverEndPointBEList);
	}

	private JAXBElement<TextTokenSubstitutionBEList> generateTextTokens(Kanal kanal, String tekst, String tittel) {
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

	// Liste over errorID: https://altinn.github.io/docs/api/tjenesteeiere/soap/grensesnitt/varseltjeneste/#feilsituasjoner
	private String constructAltinnErrorMessage(INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage e) {
		AltinnFault faultInfo = e.getFaultInfo();

		if (faultInfo == null) {
			return e.getMessage();
		}

		return "errorGuid=" + unwrap(faultInfo.getErrorGuid()) + ", " +
			   "userGuid=" + unwrap(faultInfo.getUserGuid()) + ", " +
			   "errorId=" + faultInfo.getErrorID() + ", " +
			   "errorMessage=" + unwrap(faultInfo.getAltinnErrorMessage());
	}

	private Integer getFeilkode(INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage e) {
		AltinnFault faultInfo = e.getFaultInfo();
		if (faultInfo == null) {
			return null;
		}

		return faultInfo.getErrorID();
	}

	private String unwrap(JAXBElement<String> jaxbElement) {
		if (jaxbElement == null) {
			return "null";
		}

		return jaxbElement.getValue();
	}
}
