package no.nav.doknotifikasjon.knot002.itest;


import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.standalonenotificationbe._2009._10.StandaloneNotificationBEList;
import no.altinn.services.common.fault._2009._10.AltinnFault;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.knot002.itest.utils.DoknotifikasjonStatusMatcher;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.AbstractKafkaBrokerTest;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_SMS;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.KONTAKTINFO;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus;
import static no.nav.doknotifikasjon.knot002.itest.utils.TestUtils.generateAltinnResponse;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;
import static no.nav.doknotifikasjon.kodeverk.Status.OVERSENDT;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("altinn2")
class Knot002ITest extends AbstractKafkaBrokerTest {

	@Autowired
	private NotifikasjonRepository notifikasjonRepository;

	@Autowired
	private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@MockitoSpyBean
	private KafkaEventProducer kafkaEventProducer;

	@MockitoBean
	private INotificationAgencyExternalBasic iNotificationAgencyExternalBasic;

	@BeforeEach
	void setup() {
		notifikasjonDistribusjonRepository.deleteAll();
		notifikasjonRepository.deleteAll();
		reset(kafkaEventProducer);
	}

	@Test
	void shouldSetFerdigstilltStatusOnHappyPath() throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		when(iNotificationAgencyExternalBasic.sendStandaloneNotificationBasicV3(anyString(), anyString(), any(StandaloneNotificationBEList.class))).thenReturn(generateAltinnResponse(TransportType.SMS, KONTAKTINFO));
		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), OPPRETTET, SMS));
		Integer id = notifikasjonDistribusjon.getId();

		DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(id);
		putMessageOnKafkaTopic(doknotifikasjonSms);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			NotifikasjonDistribusjon updatedNotifikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(id).orElseThrow(() -> new RuntimeException("Failed test!"));

			assertNotNull(updatedNotifikasjonDistribusjon);
			assertEquals(FERDIGSTILT, updatedNotifikasjonDistribusjon.getStatus());
			assertEquals(SMS, updatedNotifikasjonDistribusjon.getKanal());
			assertEquals("teamdokumenthandtering", updatedNotifikasjonDistribusjon.getEndretAv());
			assertNotNull(updatedNotifikasjonDistribusjon.getEndretDato());

			verify(kafkaEventProducer).publish(
					eq(KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS),
					argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FERDIGSTILT", "notifikasjon sendt via sms", id))
			);
		});
	}

	@Test
	void shouldAbortIfDistribusjonNotFound() {
		DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(1234);
		putMessageOnKafkaTopic(doknotifikasjonSms);
		NotifikasjonDistribusjon updatedNotifikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(1234).orElse(null);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			assertNull(updatedNotifikasjonDistribusjon);
			verify(kafkaEventProducer, times(0)).publish(
					eq(KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS),
					any()
			);
		});

	}

	@Test
	void shouldWriteToStatusQueueIfStatusIsInvalid() {
		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), OVERSENDT, SMS));
		Integer id = notifikasjonDistribusjon.getId();
		DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(id);

		putMessageOnKafkaTopic(doknotifikasjonSms);

		await().atMost(10, SECONDS).untilAsserted(() ->
				verify(kafkaEventProducer, atLeastOnce()).publish(
						eq(KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS),
						argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FEILET", "distribusjon til sms feilet: ugyldig status", id))
				)
		);
	}

	@Test
	void shouldWriteToStatusQueueIfKanalIsInvalid() {
		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), OPPRETTET, Kanal.EPOST));
		Integer id = notifikasjonDistribusjon.getId();
		DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(id);

		putMessageOnKafkaTopic(doknotifikasjonSms);

		await().atMost(10, SECONDS).untilAsserted(() ->
				verify(kafkaEventProducer, atLeastOnce()).publish(
						eq(KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS),
						argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FEILET", "distribusjon til sms feilet: ugyldig kanal", id))
				)
		);
	}

	@Test
	void shouldWriteToStatusQueueIfAltinnThrowsFunctionalError() throws INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage {
		var altinnFault = new AltinnFault();
		altinnFault.setAltinnErrorMessage(constructJaxbElement("AltinnErrorMessage", "Ugyldig norsk mobiltelefonnummer."));
		altinnFault.setErrorGuid(constructJaxbElement("ErrorGuid", "fedcba"));
		altinnFault.setErrorID(30303);
		altinnFault.setUserGuid(constructJaxbElement("UserGuid", "abcdef"));

		INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage altinnException = new INotificationAgencyExternalBasicSendStandaloneNotificationBasicV3AltinnFaultFaultFaultMessage(
				"Feil i altinn",
				altinnFault
		);
		when(iNotificationAgencyExternalBasic.sendStandaloneNotificationBasicV3(anyString(), anyString(), any(StandaloneNotificationBEList.class))).thenThrow(altinnException);
		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), OPPRETTET, SMS));
		Integer id = notifikasjonDistribusjon.getId();
		DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(id);

		putMessageOnKafkaTopic(doknotifikasjonSms);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			notifikasjonDistribusjonRepository.findById(id).orElseThrow(() -> new RuntimeException("Failed test!"));

			verify(kafkaEventProducer, atLeastOnce()).publish(
					eq(KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS),
					argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FEILET",
							"Funksjonell feil i kall mot Altinn. errorGuid=fedcba, userGuid=abcdef, errorId=30303, errorMessage=Ugyldig norsk mobiltelefonnummer.", id))
			);
		});
	}

	private void putMessageOnKafkaTopic(DoknotifikasjonSms doknotifikasjonSms) {
		kafkaEventProducer.publish(
				KAFKA_TOPIC_DOK_NOTIFIKASJON_SMS,
				doknotifikasjonSms
		);
	}
}
