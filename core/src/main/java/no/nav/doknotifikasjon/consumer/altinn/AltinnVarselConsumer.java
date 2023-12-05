package no.nav.doknotifikasjon.consumer.altinn;

import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import no.altinn.schemas.services.serviceengine.standalonenotificationbe._2009._10.StandaloneNotificationBEList;
import no.altinn.services.common.fault._2009._10.AltinnFault;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalEC2;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalEC2SendStandaloneNotificationECV3AltinnFaultFaultFaultMessage;
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

import static java.lang.String.format;
import static no.nav.doknotifikasjon.consumer.altinn.AltinnFunksjonellFeil.erFunksjonellFeil;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_ALTIN_CONSUMER;

@Slf4j
@Service
public class AltinnVarselConsumer {
	private final INotificationAgencyExternalEC2 iNotificationAgencyExternalEC2;
	private final AltinnProps altinnProps;

	public AltinnVarselConsumer(INotificationAgencyExternalEC2 iNotificationAgencyExternalEC2,
								AltinnProps altinnProps) {
		this.iNotificationAgencyExternalEC2 = iNotificationAgencyExternalEC2;
		this.altinnProps = altinnProps;
	}

	@Metrics(value = DOK_ALTIN_CONSUMER, createErrorMetric = true, errorMetricInclude = AltinnTechnicalException.class)
	@Retryable(
			retryFor = AltinnTechnicalException.class,
			maxAttemptsExpression = "${retry.attempts:10}",
			backoff = @Backoff(delayExpression = "${retry.delay:1000}", multiplier = 2, maxDelay = 60_000L)
	)
	public void sendVarsel(Kanal kanal, String kontaktInfo, String fnr, String tekst, String tittel) {
		if (!altinnProps.sendTilAltinn()) {
			log.info("Sender ikke melding til Altinn. flagget sendTilAltinn=false");
			return;
		}

		StandaloneNotificationBEList standaloneNotification = StandaloneNotificationMapper.map(kanal, kontaktInfo, fnr, tekst, tittel);

		try {
			iNotificationAgencyExternalEC2.sendStandaloneNotificationECV3(
					altinnProps.username(),
					altinnProps.password(),
					standaloneNotification
			);
		} catch (INotificationAgencyExternalEC2SendStandaloneNotificationECV3AltinnFaultFaultFaultMessage e) {
			final String altinnErrorMessage = constructAltinnErrorMessage(e.getFaultInfo());

			Integer feilkode = getFeilkode(e.getFaultInfo());
			if (erFunksjonellFeil(feilkode)) {
				throw new AltinnFunctionalException(format("Funksjonell feil i kall mot Altinn. %s", altinnErrorMessage), e);
			} else {
				if (erUhandterbarTekniskFeil(e.getFaultInfo())) {
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

	private boolean erUhandterbarTekniskFeil(AltinnFault faultInfo) {
		if (faultInfo != null) {
			log.error("Utvidet teknisk feil info errorGuid={}, altinnLocalizedErrorMessage={}, altinnExtendedErrorMessage={}",
					unwrap(faultInfo.getErrorGuid()), unwrap(faultInfo.getAltinnLocalizedErrorMessage()), unwrap(faultInfo.getAltinnExtendedErrorMessage()));
			return unwrap(faultInfo.getAltinnLocalizedErrorMessage()).contains("Object reference not set to an instance of an object");
		}

		return false;
	}

	// Liste over errorID: https://altinn.github.io/docs/api/tjenesteeiere/soap/grensesnitt/varseltjeneste/#feilsituasjoner
	private String constructAltinnErrorMessage(AltinnFault faultInfo) {
		return "errorGuid=" + unwrap(faultInfo.getErrorGuid()) + ", " +
				"userGuid=" + unwrap(faultInfo.getUserGuid()) + ", " +
				"errorId=" + faultInfo.getErrorID() + ", " +
				"errorMessage=" + unwrap(faultInfo.getAltinnErrorMessage());
	}

	private Integer getFeilkode(AltinnFault faultInfo) {
		return faultInfo == null ? null : faultInfo.getErrorID();
	}

	private String unwrap(JAXBElement<String> jaxbElement) {
		return jaxbElement == null ? "null" : jaxbElement.getValue();
	}
}
