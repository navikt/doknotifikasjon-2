package no.nav.doknotifikasjon.consumer;

import no.altinn.schemas.services.serviceengine.notification._2015._06.SendNotificationResultList;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.doknotifikasjon.consumer.altinn.AltinnVarselConsumer;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.FieldSetter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AltinnVarselConsumerTest {

	@Test
	void serviceShouldSendToAltinn() throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		AltinnProps altinnProps = mock(AltinnProps.class);
		INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = mock(INotificationAgencyExternalBasic.class);
		AltinnVarselConsumer consumer = new AltinnVarselConsumer(iNotificationAgencyExternalBasic, altinnProps);
		doReturn(new SendNotificationResultList()).when(iNotificationAgencyExternalBasic).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
		doReturn("testname").when(altinnProps).getUsername();
		doReturn("testpassword").when(altinnProps).getPassword();
		consumer.sendVarsel(Kanal.EPOST, null, null, null, null);
		verify(iNotificationAgencyExternalBasic, times(1)).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
	}

	@Test
	void serviceShouldNotSendToAltinn() throws NoSuchFieldException, INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		AltinnProps altinnProps = mock(AltinnProps.class);
		INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = mock(INotificationAgencyExternalBasic.class);
		AltinnVarselConsumer consumer = new AltinnVarselConsumer(iNotificationAgencyExternalBasic, altinnProps);
		FieldSetter.setField(consumer, consumer.getClass().getDeclaredField("sendTilAltinn"), false);
		consumer.sendVarsel(Kanal.EPOST, null, null, null, null);
		verify(iNotificationAgencyExternalBasic, never()).sendStandaloneNotificationBasicV3(anyString(), anyString(), any());
	}
}
