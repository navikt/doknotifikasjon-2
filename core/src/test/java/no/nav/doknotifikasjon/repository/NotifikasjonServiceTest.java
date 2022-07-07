package no.nav.doknotifikasjon.repository;

import no.nav.doknotifikasjon.repository.utils.ApplicationTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertNull;
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
	void shouldRetryAndReturnNullWhenBestillingsIdDoesNotExist() {
		when(notifikasjonRepository.findByBestillingsId(BESTILLINGS_ID)).thenReturn(empty());

		var result = notifikasjonService.findByBestillingsId(BESTILLINGS_ID);

		verify(notifikasjonRepository, times(3)).findByBestillingsId(BESTILLINGS_ID);
		assertNull(result);
	}
}