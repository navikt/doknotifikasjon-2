package no.nav.doknotifikasjon.consumer.altinn;

import lombok.extern.slf4j.Slf4j;
import no.altinn.springsoap.client.gen.SendStandaloneNotificationBasicV3Response;
import no.altinn.springsoap.client.gen.SendStandaloneNotificationBasicV3;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.SoapFaultClientException;

import javax.xml.namespace.QName;
import java.util.Optional;

import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_SHORT;
import static no.nav.doknotifikasjon.constants.RetryConstants.MULTIPLIER_SHORT;

@Slf4j
public class AltinnConsumer extends WebServiceGatewaySupport {

	private final int MAX_ATTEMPTS = 3;

	private String username;
	private String password;

	public AltinnConsumer(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public void sendStandaloneNotificationV3(Kanal kanal, String kontaktInfo, String tekst) {
		sendStandaloneNotificationV3(kanal, kontaktInfo, tekst, "");
	}

	@Retryable(include = AltinnTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void sendStandaloneNotificationV3(Kanal kanal, String kontaktInfo, String tekst, String tittel) {


		try {
			SendStandaloneNotificationBasicV3 request = AltinnRequestMapper.createRequest(kanal, kontaktInfo, tekst, tittel, password, username);

			SendStandaloneNotificationBasicV3Response response = (SendStandaloneNotificationBasicV3Response) getWebServiceTemplate().marshalSendAndReceive(request);

			if (!AltinnResponseValidator.isValidResponse(kanal, kontaktInfo, response)) {
				throw new AltinnFunctionalException("SendStandaloneNotificationBasicV3Response inneholder ikke notifikasjon som ble forsøkt sendt");
			}

		} catch (AltinnFunctionalException exception) {
			log.error("Det oppstod en funksjonell feil i sendStandaloneNotificationV3 ved sending av request feilmelding={}", exception.getMessage(), exception);
			throw exception;
		} catch (SoapFaultClientException exception) {
			log.error(
					"Det oppstod en SoapFault i sendStandaloneNotificationV3 ved sending av request: feilkode={} feilmelding={}",
					Optional.of(exception).map(SoapFaultClientException::getFaultCode).map(QName::getLocalPart).orElse(null),
					exception.getFaultStringOrReason(),
					exception
			);
			throw exception;
		} catch (Exception exception) {
			log.error("sendStandaloneNotificationV3 ukjent feil, feilmelding={}", exception.getMessage(), exception);
			throw new AltinnTechnicalException("sendStandaloneNotificationV3 ukjent feil, feilmelding=" + exception.getMessage(), exception);
		}
	}
}
