package no.nav.doknotifikasjon.consumer.test;

import no.nav.doknotifikasjon.consumer.Knot006Service;
import no.nav.doknotifikasjon.consumer.NotifikasjonMedKontaktInfoTO;
import no.nav.doknotifikasjon.exception.functional.DuplicateNotifikasjonInDBException;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjonTO;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.INFO_ALREADY_EXIST_IN_DATABASE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {Knot006Service.class})
class Knot006ServiceTest {

	@Autowired
	Knot006Service knot006Service;

	@MockBean
	KafkaStatusEventProducer statusProducer;

	@MockBean
	NotifikasjonService notifikasjonService;

	@MockBean
	KafkaEventProducer producer;


	@Test
	void shouldGetExceptionWhenNotifikasjonWithTheSameBestillingsIdAlreadyExist() {
		NotifikasjonMedKontaktInfoTO doknotifikasjon = createDoknotifikasjonTO();

		when(notifikasjonService.existsByBestillingsId(anyString()))
				.thenReturn(true);

		assertThrows(DuplicateNotifikasjonInDBException.class, () ->
				knot006Service.createNotifikasjonByNotifikasjonMedKontaktInfoTO(doknotifikasjon)
		);

		verify(statusProducer).publishDoknotifikasjonStatusInfo(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), INFO_ALREADY_EXIST_IN_DATABASE, null
		);
	}
}