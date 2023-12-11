package no.nav.doknotifikasjon.consumer;

import no.altinn.services.common.fault._2009._10.AltinnFault;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalEC2;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalEC2SendStandaloneNotificationECAltinnFaultFaultFaultMessage;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.doknotifikasjon.consumer.altinn.AltinnFunksjonellFeil;
import no.nav.doknotifikasjon.consumer.altinn.AltinnVarselConsumer;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import no.nav.doknotifikasjon.repository.utils.ApplicationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {ApplicationTestConfig.class})
@ActiveProfiles({"itest"})
public class AltinnVarselConsumerRetryTest {

	@MockBean
	INotificationAgencyExternalEC2 iNotificationAgencyExternalEC2;

	@MockBean
	AltinnProps altinnProps;

	@Autowired
	AltinnVarselConsumer consumer;

	@BeforeEach
	public void beforeEach() {
		when(altinnProps.username()).thenReturn("username");
		when(altinnProps.password()).thenReturn("password");
		when(altinnProps.sendTilAltinn()).thenReturn(true);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 44})
	void shouldRetryIfTechnicalError(Integer feilkode) throws INotificationAgencyExternalEC2SendStandaloneNotificationECAltinnFaultFaultFaultMessage {
		var altinnFault = new AltinnFault();
		altinnFault.setErrorID(feilkode);
		doThrow(new INotificationAgencyExternalEC2SendStandaloneNotificationECAltinnFaultFaultFaultMessage(
				"Feil i altinn", altinnFault
		)).when(iNotificationAgencyExternalEC2).sendStandaloneNotificationEC(anyString(), anyString(), any());


		assertThrows(AltinnTechnicalException.class, () -> consumer.sendVarsel(EPOST, null, null, null, null));

		verify(iNotificationAgencyExternalEC2, times(5)).sendStandaloneNotificationEC(anyString(), anyString(), any());
	}

	@ParameterizedTest
	@EnumSource(AltinnFunksjonellFeil.class)
	void shouldNotRetryIfFunctionalError(AltinnFunksjonellFeil feil) throws INotificationAgencyExternalEC2SendStandaloneNotificationECAltinnFaultFaultFaultMessage {
		var altinnFault = new AltinnFault();
		altinnFault.setErrorID(feil.feilkode);
		doThrow(new INotificationAgencyExternalEC2SendStandaloneNotificationECAltinnFaultFaultFaultMessage(
				"Feil i altinn",
				altinnFault)).when(iNotificationAgencyExternalEC2).sendStandaloneNotificationEC(anyString(), anyString(), any());

		assertThrows(AltinnFunctionalException.class, () -> consumer.sendVarsel(EPOST, null, null, null, null));

		verify(iNotificationAgencyExternalEC2, times(1)).sendStandaloneNotificationEC(anyString(), anyString(), any());
	}

}
