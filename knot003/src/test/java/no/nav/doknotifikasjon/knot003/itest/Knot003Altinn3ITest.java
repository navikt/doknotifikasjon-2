package no.nav.doknotifikasjon.knot003.itest;


import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.nav.doknotifikasjon.consumer.altinn3.Altinn3TokenExchangeConsumer;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.knot003.itest.utils.DoknotifikasjonStatusMatcher;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.AbstractKafkaBrokerTest;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_EPOST;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS;
import static no.nav.doknotifikasjon.knot003.itest.utils.TestUtils.createNotifikasjon;
import static no.nav.doknotifikasjon.knot003.itest.utils.TestUtils.createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;
import static no.nav.doknotifikasjon.kodeverk.Status.OVERSENDT;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ActiveProfiles({ "wiremock", "altinn3" })
class Knot003Altinn3ITest extends AbstractKafkaBrokerTest {

	@Autowired
	private NotifikasjonRepository notifikasjonRepository;

	@Autowired
	private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@MockitoSpyBean
	private KafkaEventProducer kafkaEventProducer;

	@MockitoBean
	private INotificationAgencyExternalBasic iNotificationAgencyExternalBasic;

	@MockitoBean
	private Altinn3TokenExchangeConsumer altinn3TokenExchangeConsumer;


	@BeforeEach
	void setup() {
		notifikasjonDistribusjonRepository.deleteAll();
		notifikasjonRepository.deleteAll();
		reset(kafkaEventProducer);

		when(altinn3TokenExchangeConsumer.getAltinnToken(any(), any())).thenReturn("token");
	}

	@Test
	void shouldSetFerdigstilltStatusOnHappyPath() {
		stubFor(post(urlEqualTo("/altinn-order-notification"))
			.willReturn(aResponse()
				.withStatus(OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("altinn3/order-notification-ok.json")));

		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), OPPRETTET, EPOST));
		Integer id = notifikasjonDistribusjon.getId();

		DoknotifikasjonEpost doknotifikasjonEpost = new DoknotifikasjonEpost(id);
		putMessageOnKafkaTopic(doknotifikasjonEpost);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			NotifikasjonDistribusjon updatedNotifikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(id).orElseThrow(() -> new RuntimeException("Failed test!"));
			assertNotNull(updatedNotifikasjonDistribusjon);
			assertEquals(FERDIGSTILT, updatedNotifikasjonDistribusjon.getStatus());
			assertEquals(EPOST, updatedNotifikasjonDistribusjon.getKanal());
			assertEquals("teamdokumenthandtering", updatedNotifikasjonDistribusjon.getEndretAv());
			assertNotNull(updatedNotifikasjonDistribusjon.getEndretDato());

			verify(kafkaEventProducer).publish(
					eq(KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS),
					argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FERDIGSTILT", "notifikasjon sendt via epost", id))
			);
			verify(altinn3TokenExchangeConsumer, times(1)).getAltinnToken(any(), any());
		});
	}

	@Test
	void shouldAbortIfDistribusjonNotFound() {
		DoknotifikasjonEpost doknotifikasjonEpost = new DoknotifikasjonEpost(1234);
		putMessageOnKafkaTopic(doknotifikasjonEpost);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			NotifikasjonDistribusjon updatedNotifikasjonDistribusjon = notifikasjonDistribusjonRepository.findById(1234).orElse(null);

			assertNull(updatedNotifikasjonDistribusjon);
			verify(kafkaEventProducer, times(0)).publish(
					eq(KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS),
					any()
			);
		});
	}

	@Test
	void shouldWriteToStatusQueueIfStatusIsInvalid() {
		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), OVERSENDT, EPOST));
		Integer id = notifikasjonDistribusjon.getId();
		DoknotifikasjonEpost doknotifikasjonEpost = new DoknotifikasjonEpost(id);

		putMessageOnKafkaTopic(doknotifikasjonEpost);
		await().atMost(10, SECONDS).untilAsserted(() ->
				verify(kafkaEventProducer, atLeastOnce()).publish(
						eq(KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS),
						argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FEILET", "distribusjon til epost feilet: ugyldig status", id))
				)
		);
	}

	@Test
	void shouldWriteToStatusQueueIfKanalIsInvalid() {
		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), OPPRETTET, SMS));
		Integer id = notifikasjonDistribusjon.getId();

		DoknotifikasjonEpost doknotifikasjonEpost = new DoknotifikasjonEpost(id);
		putMessageOnKafkaTopic(doknotifikasjonEpost);

		await().atMost(10, SECONDS).untilAsserted(() ->
				verify(kafkaEventProducer, atLeastOnce()).publish(
						eq(KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS),
						argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FEILET", "distribusjon til epost feilet: ugyldig kanal", id))
				)
		);
	}

	@Test
	void shouldWriteToStatusQueueIfAltinnThrowsFunctionalError() {
		stubFor(post(urlEqualTo("/altinn-order-notification"))
			.willReturn(aResponse()
				.withStatus(UNPROCESSABLE_ENTITY.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("altinn3/order-notification-notok.json")));

		NotifikasjonDistribusjon notifikasjonDistribusjon = notifikasjonDistribusjonRepository.saveAndFlush(createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(createNotifikasjon(), OPPRETTET, EPOST));
		Integer id = notifikasjonDistribusjon.getId();

		DoknotifikasjonEpost doknotifikasjonEpost = new DoknotifikasjonEpost(id);
		putMessageOnKafkaTopic(doknotifikasjonEpost);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			notifikasjonDistribusjonRepository.findById(id).orElseThrow(() -> new RuntimeException("Failed test!"));

			verify(kafkaEventProducer, atLeastOnce()).publish(
					eq(KAFKA_TOPIC_DOK_NOTIFIKASJON_STATUS),
					argThat(new DoknotifikasjonStatusMatcher("teamdokumenthandtering", "1234-5678-9101", "FEILET",
						"Funksjonell feil i kall mot Altinn. 422 UNPROCESSABLE_ENTITY, errorTitle=NOT-00001, errorMessage=Ugyldig norsk mobiltelefonnummer.", id))
			);
		});
	}

	private void putMessageOnKafkaTopic(DoknotifikasjonEpost doknotifikasjonEpost) {
		kafkaEventProducer.publish(
				KAFKA_TOPIC_DOK_NOTIFIKASJON_EPOST,
				doknotifikasjonEpost
		);
	}
}
