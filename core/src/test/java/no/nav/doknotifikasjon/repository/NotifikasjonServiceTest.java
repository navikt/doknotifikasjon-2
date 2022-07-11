package no.nav.doknotifikasjon.repository;

import no.nav.doknotifikasjon.repository.utils.ApplicationTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.test.context.ActiveProfiles;

import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {ApplicationTestConfig.class})
@ActiveProfiles({"itest"})
class NotifikasjonServiceTest {

	private static final String BESTILLINGS_ID = "12345";

	@MockBean
	private NotifikasjonRepository notifikasjonRepository;

	@Autowired
	private NotifikasjonService notifikasjonService;

	@Test
	void shouldRetryAndReturnNullWhenBestillingsIdDoesNotExistForKnot004() {
		when(notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID)).thenReturn(empty());

		var result = notifikasjonService.findByBestillingsId(BESTILLINGS_ID);

		verify(notifikasjonRepository, times(3)).findByBestillingsId(BESTILLINGS_ID);
		assertNull(result);
	}

	@Test
	void shouldNotRetryWhenBestillingsIdDoesNotExistForKnot005() {
		when(notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID)).thenReturn(empty());

		var result = notifikasjonService.findByBestillingsIdIngenRetryForNotifikasjonIkkeFunnet(BESTILLINGS_ID);

		verify(notifikasjonRepository, times(1)).findByBestillingsId(BESTILLINGS_ID);
		assertNull(result);
	}

	@Test
	void shouldRetryForNonSpecifiedExceptionsInKnot005() {
		when(notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID)).thenThrow(new DataAccessException("Feil i databasekall"){ });

		Exception e = assertThrows(ExhaustedRetryException.class, () -> notifikasjonService.findByBestillingsIdIngenRetryForNotifikasjonIkkeFunnet(BESTILLINGS_ID));

		assertEquals(ExhaustedRetryException.class, e.getClass());
		verify(notifikasjonRepository, times(5)).findByBestillingsId(BESTILLINGS_ID);
	}

}