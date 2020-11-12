package no.nav.doknotifikasjon.knot002.itest;


import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.EmbededKafkaBroker;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class Knot002ITest extends EmbededKafkaBroker {

	/*@Autowired
	private KafkaEventProducer KafkaEventProducer;

	@Autowired
	private NotifikasjonRepository notifikasjonRepository;

	@Autowired
	private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@SpyBean
	private Knot002NotifikasjonEntityMapper NotifikasjonEntityMapper;

	@SpyBean
	private KafkaEventProducer kafkaEventProducer;

	@BeforeEach
	public void setup() {
		notifikasjonDistribusjonRepository.deleteAll();
		notifikasjonRepository.deleteAll();
		reset(kafkaEventProducer);
	}

	@Test
	void shouldSetFerdigstilltStatusOnHappyPath() {


		when(webServiceTemplateMock.marshalSendAndReceive(any())).thenReturn(generateAltinnResponse(TransportType.SMS, KONTAKTINFO));

		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), Status.OPPRETTET, Kanal.SMS));

		Integer id = notifikasjonDistribusjon.getId();

		DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(id);
		putMessageOnKafkaTopic(doknotifikasjonSms);

		NotifikasjonDistribusjon updatedNotifikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(id).orElseThrow(() -> new RuntimeException("Failed test!"));


		assertEquals(Status.FERDIGSTILT, updatedNotifikasjonDistribusjon.getStatus());
		assertEquals(Kanal.SMS, updatedNotifikasjonDistribusjon.getKanal());
		assertEquals("teamdokumenthandtering", updatedNotifikasjonDistribusjon.getEndretAv());
		assertNotNull(updatedNotifikasjonDistribusjon.getEndretDato());

		verify(kafkaEventProducer).publish(
				eq(KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS),
				eq(String.valueOf(id)),
				argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FERDIGSTILT", "notifikasjon sendt via sms", id)),
				anyLong()
		);
	}

	@Test
	void shouldAbortIfDistribusjonNotFound() {

		DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(1234);
		putMessageOnKafkaTopic(doknotifikasjonSms);

		NotifikasjonDistribusjon updatedNotifikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(1234).orElse(null);

		assertNull(updatedNotifikasjonDistribusjon);
		try {
			//needed because backoff
			TimeUnit.SECONDS.sleep(10);
			verify(NotifikasjonEntityMapper, times(3)).mapNotifikasjonDistrubisjon(1234);
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	void shouldWriteToStatusQueueIfStatusIsInvalid() {

		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), Status.OVERSENDT, Kanal.SMS));

		Integer id = notifikasjonDistribusjon.getId();

		DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(id);
		putMessageOnKafkaTopic(doknotifikasjonSms);

		verify(kafkaEventProducer, atLeastOnce()).publish(
				eq(KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS),
				eq(String.valueOf(id)),
				argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FEILET", "distribusjon til sms feilet: ugyldig status", id)),
				anyLong()
		);
	}

	@Test
	void shouldWriteToStatusQueueIfKanalIsInvalid() {

		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), Status.OPPRETTET, Kanal.EPOST));

		Integer id = notifikasjonDistribusjon.getId();

		DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(id);
		putMessageOnKafkaTopic(doknotifikasjonSms);

		verify(kafkaEventProducer, atLeastOnce()).publish(
				eq(KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS),
				eq(String.valueOf(id)),
				argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FEILET", "distribusjon til sms feilet: ugyldig kanal", id)),
				anyLong()
		);
	}

	@Test
	void shouldWriteToStatusQueueIfAltinnThrowsFunctionalError() {


		when(webServiceTemplateMock.marshalSendAndReceive(any())).thenThrow(new AltinnFunctionalException("Altinn Functional Exception"));

		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), Status.OPPRETTET, Kanal.SMS));

		Integer id = notifikasjonDistribusjon.getId();

		DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(id);
		putMessageOnKafkaTopic(doknotifikasjonSms);

		NotifikasjonDistribusjon updatedNotifikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(id).orElseThrow(() -> new RuntimeException("Failed test!"));


		verify(kafkaEventProducer, atLeastOnce()).publish(
				eq(KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS),
				eq(String.valueOf(id)),
				argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FEILET", "Altinn Functional Exception", id)),
				anyLong()
		);
	}

	private void putMessageOnKafkaTopic(DoknotifikasjonSms doknotifikasjonSms) {
		try {
			Long keyGenerator = System.currentTimeMillis();

			KafkaEventProducer.publish(
					KAFKA_TOPIC_DOK_NOTIFKASJON_SMS,
					doknotifikasjonSms,
					keyGenerator
			);

			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException exception) {
			fail();
		}
	}

 */
}