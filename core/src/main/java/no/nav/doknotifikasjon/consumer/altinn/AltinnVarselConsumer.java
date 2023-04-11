package no.nav.doknotifikasjon.consumer.altinn;

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

import javax.xml.bind.JAXBElement;
import java.util.List;

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
			include = AltinnTechnicalException.class,
			maxAttemptsExpression = "${retry.attempts:10}",
			backoff = @Backoff(delayExpression = "${retry.delay:1000}", multiplier = 2, maxDelay = 60_000L)
	)
	public void sendVarsel(Kanal kanal, String kontaktInfo, String fnr, String tekst, String tittel) {
		if (!sendTilAltinn) {
			log.info("Sender ikke melding til Altinn. flagget sendTilAltinn=false");
			return;
		}
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
			final String altinnErrorMessage = constructAltinnErrorMessage(e);

			Integer feilkode = getFeilkode(e);
			if (erFunksjonellFeil(feilkode)) {
				throw new AltinnFunctionalException(format("Funksjonell feil i kall mot Altinn. %s", altinnErrorMessage), e);
			} else {
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

	@Recover
	public void altinnTechnicalExceptionRecovery(AltinnTechnicalException e, Kanal kanal, String kontaktInfo, String fnr, String tekst, String tittel) {
		log.warn("Teknisk feil for sending av sms/epost til Altinn - maks. antall forsøk brukt");
		throw e;
	}

	// Catch-all for alle andre exceptions - hvis ikke blir ExhaustedRetryException kastet med meldingen 'Cannot locate recovery method'
	@Recover
	public void otherExceptionsRecovery(RuntimeException e, Kanal kanal, String kontaktInfo, String fnr, String tekst, String tittel) {
		throw e;
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
		if (kanal == SMS) {
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
		if (kanal == EPOST) {
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
		if (SMS == kanal) return TransportType.SMS;
		if (EPOST == kanal) return TransportType.EMAIL;
		throw new AltinnFunctionalException("Kanal er verken SMS eller EMAIL, kanal=" + kanal);
	}

	// Liste over errorID: https://altinn.github.io/docs/api/tjenesteeiere/soap/grensesnitt/varseltjeneste/#feilsituasjoner
	private String constructAltinnErrorMessage(INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage e) {
		AltinnFault faultInfo = e.getFaultInfo();

		log.warn("Altinn localized error message for errorGuid={}: {}", unwrap(faultInfo.getErrorGuid()), unwrap(faultInfo.getAltinnLocalizedErrorMessage()));
		log.warn("Altinn extended error message for errorGuid={}: {}", unwrap(faultInfo.getErrorGuid()), unwrap(faultInfo.getAltinnExtendedErrorMessage()));

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
