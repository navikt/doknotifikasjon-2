package no.nav.doknotifikasjon.repository;

import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalEC2;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonDistribusjonIkkeFunnetException;
import no.nav.doknotifikasjon.repository.utils.ApplicationTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {ApplicationTestConfig.class})
@ActiveProfiles({"itest"})
class NotifikasjonDistribusjonServiceTest {

	private static final int NOTIFIKASJONDISTRIBUSJON_ID = 12345;
	@MockBean
	INotificationAgencyExternalEC2 iNotificationAgencyExternalEC2;
	@MockBean
	private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@Autowired
	private NotifikasjonDistribusjonService notifikasjonDistribusjonService;

	@Test
	void shouldRetryForAllExceptionsInKnot002And003() {
		when(notifikasjonDistribusjonRepository.findById(NOTIFIKASJONDISTRIBUSJON_ID)).thenThrow(DoknotifikasjonDistribusjonIkkeFunnetException.class);

		Exception e = assertThrows(DoknotifikasjonDistribusjonIkkeFunnetException.class, () -> notifikasjonDistribusjonService.findById(NOTIFIKASJONDISTRIBUSJON_ID));

		assertEquals(DoknotifikasjonDistribusjonIkkeFunnetException.class, e.getClass());
		verify(notifikasjonDistribusjonRepository, times(5)).findById(NOTIFIKASJONDISTRIBUSJON_ID);
	}

}