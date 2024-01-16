package no.nav.doknotifikasjon.consumer;

import no.altinn.services.common.fault._2009._10.AltinnFault;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {ApplicationTestConfig.class})
@ActiveProfiles({"itest"})
public class AltinnVarselConsumerRetryTest {

	@MockBean
	INotificationAgencyExternalBasic iNotificationAgencyExternalBasic;

	@MockBean
	AltinnProps altinnProps;

	@Autowired
	AltinnVarselConsumer consumer;

	@BeforeEach
	public void beforeEach() {
		when(altinnProps.getUsername()).thenReturn("username");
		when(altinnProps.getPassword()).thenReturn("password");
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 44})
	void shouldRetryIfTechnicalError(Integer feilkode) throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		var altinnFault = new AltinnFault();
		altinnFault.setErrorID(feilkode);
		when(iNotificationAgencyExternalBasic.sendStandaloneNotificationBasicV3(anyString(), anyString(), any()))
				.thenThrow(new INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage(
						"Feil i altinn", altinnFault
						));

		assertThrows(AltinnTechnicalException.class, () -> consumer.sendVarsel(EPOST, null, null, null, null));

		verify(iNotificationAgencyExternalBasic, times(5)).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
	}

	@ParameterizedTest
	@EnumSource(AltinnFunksjonellFeil.class)
	void shouldNotRetryIfFunctionalError(AltinnFunksjonellFeil feil) throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		var altinnFault = new AltinnFault();
		altinnFault.setErrorID(feil.feilkode);
		when(iNotificationAgencyExternalBasic.sendStandaloneNotificationBasicV3(anyString(), anyString(), any()))
				.thenThrow(new INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage(
						"Feil i altinn",
						altinnFault)
				);

		assertThrows(AltinnFunctionalException.class, () -> consumer.sendVarsel(EPOST, null, null, null, null));

		verify(iNotificationAgencyExternalBasic, times(1)).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
	}

}
