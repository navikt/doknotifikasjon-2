package no.nav.doknotifikasjon;

import static no.nav.doknotifikasjon.TestUtils.BESTILLER_ID_2;
import static no.nav.doknotifikasjon.TestUtils.BESTILLING_ID;
import static no.nav.doknotifikasjon.TestUtils.MELDING;
import static no.nav.doknotifikasjon.TestUtils.STATUS;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjon;
import static org.mockito.Mockito.when;

import no.nav.doknotifikasjon.KafkaProducer.KafkaDoknotifikasjonStatusProducer;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class Knot004ServiceTest {
//
//	@Mock
//	private NotifikasjonRepository notifikasjonRepository;
//
//	private final DoknotifikasjonStatusValidator doknotifikasjonStatusValidator = new DoknotifikasjonStatusValidator();
//	private final KafkaDoknotifikasjonStatusProducer kafkaDoknotifikasjonStatusProducer = new KafkaDoknotifikasjonStatusProducer();
//
//	private final Knot004Service knot004Service = new Knot004Service(notifikasjonRepository, doknotifikasjonStatusValidator, kafkaDoknotifikasjonStatusProducer);


//	@BeforeEach
//	public void setup() {
//		when(notifikasjonRepository.getByBestillingId(BESTILLING_ID)).thenReturn(createNotifikasjon());
//	}

//	@Test
//	void shouldUpdateStatus() {
//		Notifikasjon notifikasjon = createNotifikasjon();
//		when(notifikasjonRepository.getByBestillingId(BESTILLING_ID)).thenReturn(notifikasjon);
//
//		DoknotifikasjonStatusTo doknotifikasjonStatusTo = new DoknotifikasjonStatusTo(BESTILLING_ID, BESTILLER_ID_2, STATUS, MELDING, null);
//		knot004Service.shouldUpdateStatus(doknotifikasjonStatusTo);
//		notifikasjon.getStatus();
//
//	}
}