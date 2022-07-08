package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_FUNCTIONAL_EXCEPTION_DIGDIR_KRR_PROXY;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_FUNCTIONAL_EXCEPTION_SIKKERHETSNIVAA;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SIKKERHETSNIVAA;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_TECHNICAL_EXCEPTION_DATABASE;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT;
import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {Knot004Service.class})
public class Knot004ServiceTest {

	private static final String BESTILLER_ID = "BESTILLER_ID";
	private static final String BESTILLINGS_ID = "BESTILLINGS_ID";

	@Autowired
	Knot004Service knot004Service;

	@MockBean
	NotifikasjonService notifikasjonService;

	@MockBean
	Validator validator;

	@MockBean
	KafkaStatusEventProducer kafkaStatusEventProducer;

	@MockBean
	MetricService metricService;

	@ParameterizedTest
	@ValueSource(strings = {
			FEILET_SIKKERHETSNIVAA,
			FEILET_TECHNICAL_EXCEPTION_DATABASE,
			FEILET_FUNCTIONAL_EXCEPTION_SIKKERHETSNIVAA,
			FEILET_FUNCTIONAL_EXCEPTION_DIGDIR_KRR_PROXY
	})
	public void shouldReturnForSpecificFeilmeldinger(String feilmelding) {
		DoknotifikasjonStatusTo doknotifikasjonStatusTo = new DoknotifikasjonStatusTo.DoknotifikasjonStatusToBuilder()
				.bestillerId(BESTILLER_ID)
				.bestillingsId(BESTILLINGS_ID)
				.status(FEILET)
				.melding(feilmelding)
				.distribusjonId(null)
				.build();
		when(validator.erStatusInfoEllerFeiletMedSpesiellFeilmelding(doknotifikasjonStatusTo.getStatus(), doknotifikasjonStatusTo.getMelding()))
				.thenReturn(true);

		knot004Service.shouldUpdateStatus(doknotifikasjonStatusTo);

		verifyNoInteractions(notifikasjonService);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET,
			FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT,
			FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION,
	})
	public void shouldProceeedForDifferentFeilmeldinger(String feilmelding) {
		DoknotifikasjonStatusTo doknotifikasjonStatusTo = new DoknotifikasjonStatusTo.DoknotifikasjonStatusToBuilder()
				.bestillerId(BESTILLER_ID)
				.bestillingsId(BESTILLINGS_ID)
				.status(FEILET)
				.melding(feilmelding)
				.distribusjonId(null)
				.build();
		when(validator.erStatusInfoEllerFeiletMedSpesiellFeilmelding(doknotifikasjonStatusTo.getStatus(), doknotifikasjonStatusTo.getMelding()))
				.thenReturn(false);
		when(notifikasjonService.findByBestillingsId(BESTILLINGS_ID)).thenReturn(null);

		knot004Service.shouldUpdateStatus(doknotifikasjonStatusTo);

		verify(notifikasjonService, times(1)).findByBestillingsId(BESTILLINGS_ID);
	}
}