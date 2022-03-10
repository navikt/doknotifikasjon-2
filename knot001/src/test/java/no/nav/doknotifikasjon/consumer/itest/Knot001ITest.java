package no.nav.doknotifikasjon.consumer.itest;

import no.nav.doknotifikasjon.consumer.TestUtils;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.AbstractKafkaBrokerTest;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SIKKERHETSNIVAA;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.OVERSENDT_NOTIFIKASJON_PROCESSED;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.kodeverk.MottakerIdType.FNR;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.OK;

@ActiveProfiles("itestWeb")
class Knot001ITest extends AbstractKafkaBrokerTest {

	@Autowired
	private KafkaEventProducer KafkaEventProducer;

	@MockBean
	private KafkaStatusEventProducer statusProducer;

	@Autowired
	private NotifikasjonRepository notifikasjonRepository;

	@Autowired
	private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@BeforeAll
	public void beforeAll() {
		this.stubGetSecurityToken();
	}

	@BeforeEach
	public void setup() {
		notifikasjonRepository.deleteAll();
		notifikasjonDistribusjonRepository.deleteAll();
	}

	@Test
	void shouldOnlySaveEpostWhenSmsIsPreferedKanalButOnlyEpostIsValidKontaktInfo() {
		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjonWithPreferedKanalAsSms();

		this.stubGetKontaktInfoWithoutSmsInKontaktInfo();
		this.putMessageOnKafkaTopic(doknotifikasjon);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotikfikasjonStatusOversendt(
					doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
			);

			Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(doknotifikasjon.getBestillingsId());

			assertNotNull(notifikasjon);
			assertEquals(doknotifikasjon.getBestillerId(), notifikasjon.getBestillerId());
			assertEquals(doknotifikasjon.getBestillingsId(), notifikasjon.getBestillingsId());
			assertEquals(doknotifikasjon.getAntallRenotifikasjoner(), notifikasjon.getAntallRenotifikasjoner());
			assertEquals(doknotifikasjon.getRenotifikasjonIntervall(), notifikasjon.getRenotifikasjonIntervall());
			assertEquals(doknotifikasjon.getPrefererteKanaler().get(0).toString(), notifikasjon.getPrefererteKanaler());
			assertEquals(OPPRETTET, notifikasjon.getStatus());

			assertEquals(doknotifikasjon.getFodselsnummer(), notifikasjon.getMottakerId());
			assertEquals(FNR, notifikasjon.getMottakerIdType());

			List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAll();

			assertEquals(1, notifikasjonDistribusjonList.size());

			NotifikasjonDistribusjon epost = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == EPOST).findFirst().get();

			assertEquals(EPOST, epost.getKanal());
			assertEquals(notifikasjon.getBestillingsId(), epost.getOpprettetAv());
			assertEquals(notifikasjon.getId(), epost.getNotifikasjon().getId());
			assertEquals(OPPRETTET, epost.getStatus());
			assertEquals(doknotifikasjon.getEpostTekst(), epost.getTekst());
			assertEquals(doknotifikasjon.getTittel(), epost.getTittel());
		});
	}

	@Test
	void shouldRunWhenReceivingKafkaEventWithSmsAsPreferedKanal() {
		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjonWithPreferedKanalAsSms();

		this.stubGetKontaktInfo();
		this.putMessageOnKafkaTopic(doknotifikasjon);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotikfikasjonStatusOversendt(
					doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
			);

			Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(doknotifikasjon.getBestillingsId());

			assertNotNull(notifikasjon);
			assertEquals(doknotifikasjon.getBestillerId(), notifikasjon.getBestillerId());
			assertEquals(doknotifikasjon.getBestillingsId(), notifikasjon.getBestillingsId());
			assertEquals(doknotifikasjon.getAntallRenotifikasjoner(), notifikasjon.getAntallRenotifikasjoner());
			assertEquals(doknotifikasjon.getRenotifikasjonIntervall(), notifikasjon.getRenotifikasjonIntervall());
			assertEquals(doknotifikasjon.getPrefererteKanaler().get(0).toString(), notifikasjon.getPrefererteKanaler());
			assertEquals(OPPRETTET, notifikasjon.getStatus());

			assertEquals(doknotifikasjon.getFodselsnummer(), notifikasjon.getMottakerId());
			assertEquals(FNR, notifikasjon.getMottakerIdType());

			List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAll();

			assertEquals(1, notifikasjonDistribusjonList.size());

			NotifikasjonDistribusjon sms = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == SMS).findFirst().get();

			assertEquals(SMS, sms.getKanal());
			assertEquals(notifikasjon.getBestillingsId(), sms.getOpprettetAv());
			assertEquals(notifikasjon.getId(), sms.getNotifikasjon().getId());
			assertEquals(OPPRETTET, sms.getStatus());
			assertEquals(doknotifikasjon.getSmsTekst(), sms.getTekst());
			assertEquals(doknotifikasjon.getTittel(), sms.getTittel());
		});
	}

	@Test
	void shouldCreateNotifikasjonDistribusjonForBothSmsAndEpost() {
		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjon();

		this.stubGetKontaktInfo();
		this.putMessageOnKafkaTopic(doknotifikasjon);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotikfikasjonStatusOversendt(
					doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
			);

			Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(doknotifikasjon.getBestillingsId());

			assertNotNull(notifikasjon);
			assertEquals(doknotifikasjon.getBestillerId(), notifikasjon.getBestillerId());
			assertEquals(doknotifikasjon.getBestillingsId(), notifikasjon.getBestillingsId());
			assertEquals(doknotifikasjon.getAntallRenotifikasjoner(), notifikasjon.getAntallRenotifikasjoner());
			assertEquals(doknotifikasjon.getRenotifikasjonIntervall(), notifikasjon.getRenotifikasjonIntervall());
			assertEquals(doknotifikasjon.getPrefererteKanaler().get(0) + ", " + doknotifikasjon.getPrefererteKanaler().get(1), notifikasjon.getPrefererteKanaler());
			assertEquals(OPPRETTET, notifikasjon.getStatus());

			assertEquals(doknotifikasjon.getFodselsnummer(), notifikasjon.getMottakerId());
			assertEquals(FNR, notifikasjon.getMottakerIdType());

			List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAll();

			assertEquals(2, notifikasjonDistribusjonList.size());

			NotifikasjonDistribusjon epost = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == EPOST).findFirst().get();
			NotifikasjonDistribusjon sms = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == SMS).findFirst().get();

			assertEquals(EPOST, epost.getKanal());
			assertEquals(notifikasjon.getBestillingsId(), epost.getOpprettetAv());
			assertEquals(notifikasjon.getId(), epost.getNotifikasjon().getId());
			assertEquals(OPPRETTET, epost.getStatus());
			assertEquals(doknotifikasjon.getEpostTekst(), epost.getTekst());
			assertEquals(doknotifikasjon.getTittel(), epost.getTittel());

			assertEquals(SMS, sms.getKanal());
			assertEquals(notifikasjon.getBestillingsId(), sms.getOpprettetAv());
			assertEquals(notifikasjon.getId(), sms.getNotifikasjon().getId());
			assertEquals(OPPRETTET, sms.getStatus());
			assertEquals(doknotifikasjon.getSmsTekst(), sms.getTekst());
			assertEquals(doknotifikasjon.getTittel(), sms.getTittel());
		});
	}

	@Test
	void shouldPutErrorMessageOnStatusTopicWhenSikkerhetnivaaIsNot4() {
		this.stubSikkerhetsnivaa();

		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjonWithSikkerhetsnivaa(4);
		this.putMessageOnKafkaTopic(doknotifikasjon);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotikfikasjonStatusFeilet(
					doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), FEILET_SIKKERHETSNIVAA, null
			);

			assertEquals(0, notifikasjonRepository.findAll().size());
		});
	}

	@Test
	void shouldPutErrorMessageOnStatusTopicWhenDkifFails() {
		this.stubGetKontaktInfoFail();
		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjon();
		this.putMessageOnKafkaTopic(doknotifikasjon);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotikfikasjonStatusFeilet(
					doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), "Ingen kontaktinformasjon er registrert på personen", null
			);

			assertEquals(0, notifikasjonRepository.findAll().size());
		});
	}

	private void putMessageOnKafkaTopic(Doknotifikasjon doknotifikasjon) {
		KafkaEventProducer.publish(
				KAFKA_TOPIC_DOK_NOTIFIKASJON,
				doknotifikasjon
		);
	}

	private void stubGetSecurityToken() {
		stubFor(get("/securitytoken?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("stsResponse_happy.json")));
	}

	private void stubGetKontaktInfo() {
		stubFor(get(urlEqualTo("/dkif/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
						.withBodyFile("dkif_happy.json")));
	}

	private void stubGetKontaktInfoWithoutSmsInKontaktInfo() {
		stubFor(get(urlEqualTo("/dkif/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
						.withBodyFile("dkif_without_sms.json")));
	}

	private void stubGetKontaktInfoFail() {
		stubFor(get(urlEqualTo("/dkif/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
						.withBodyFile("dkif_fail.json")));
	}

	private void stubSikkerhetsnivaa() {
		stubFor(post("/sikkerhetsnivaa").willReturn(aResponse().withStatus(OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("sikkerhetsnivaa.json")));
	}
}