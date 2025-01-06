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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.OVERSENDT_NOTIFIKASJON_PROCESSED;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.kodeverk.MottakerIdType.FNR;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ActiveProfiles("wiremock")
class Knot001ITest extends AbstractKafkaBrokerTest {

	@Autowired
	private KafkaEventProducer KafkaEventProducer;

	@MockitoBean
	private KafkaStatusEventProducer statusProducer;

	@Autowired
	private NotifikasjonRepository notifikasjonRepository;

	@Autowired
	private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@BeforeEach
	public void setup() {
		notifikasjonRepository.deleteAll();
		notifikasjonDistribusjonRepository.deleteAll();
		stubAzure();
	}

	@Test
	void shouldOnlySaveEpostWhenSmsIsPreferedKanalButOnlyEpostIsValidKontaktInfo() {
		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjonWithPreferedKanalAsSms();

		this.stubGetKontaktInfoWithoutSmsInKontaktInfo();
		this.putMessageOnKafkaTopic(doknotifikasjon);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotifikasjonStatusOversendt(
					doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
			);

			Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(doknotifikasjon.getBestillingsId()).orElse(null);

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

		this.stubGetKontaktInfo("digdir_krr_proxy_happy.json");
		this.putMessageOnKafkaTopic(doknotifikasjon);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotifikasjonStatusOversendt(
					doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
			);

			Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(doknotifikasjon.getBestillingsId()).orElse(null);

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

		this.stubGetKontaktInfo("digdir_krr_proxy_happy.json");
		this.putMessageOnKafkaTopic(doknotifikasjon);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotifikasjonStatusOversendt(
					doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
			);

			Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(doknotifikasjon.getBestillingsId()).orElse(null);

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
	void shouldPutErrorMessageOnStatusTopicWhenKanVarslesIsFalseAndAntallRenotifikasjonerSatt() {
		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjon();

		this.stubGetKontaktInfo("digdir_krr_proxy_kanvarsles_false.json");
		this.putMessageOnKafkaTopic(doknotifikasjon);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotifikasjonStatusFeilet(
					doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION, null
			);
			assertEquals(0, notifikasjonRepository.findAll().size());

		});
	}

	@Test
	void shouldPutErrorMessageOnStatusTopicWhenReservertAndAntallRenotifikasjonerSatt() {
		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjon();

		this.stubGetKontaktInfo("digdir_krr_proxy_reservert.json");
		this.putMessageOnKafkaTopic(doknotifikasjon);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotifikasjonStatusFeilet(
					doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT, null
			);
			assertEquals(0, notifikasjonRepository.findAll().size());

		});
	}

	@Test
	void shouldPutErrorMessageOnStatusTopicWhenDigdirKRRProxyFails() {
		this.stubGetKontaktInfoFail();
		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjon();
		this.putMessageOnKafkaTopic(doknotifikasjon);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotifikasjonStatusFeilet(
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

	private void stubGetKontaktInfo(String path) {
		stubDigdirKRRProxyWithBodyFile(path);
	}

	private void stubGetKontaktInfoWithoutSmsInKontaktInfo() {
		stubDigdirKRRProxyWithBodyFile("digdir_krr_proxy_without_sms.json");
	}

	private void stubGetKontaktInfoFail() {
		stubDigdirKRRProxyWithBodyFile("digdir_krr_proxy_fail.json");
	}

	private void stubDigdirKRRProxyWithBodyFile(String bodyfile) {
		stubFor(post(urlEqualTo("/digdir_krr_proxy/rest/v1/personer"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(bodyfile)));
	}

	private void stubAzure() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
	}

}