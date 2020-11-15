package no.nav.doknotifikasjon.consumer.itest;

import no.nav.doknotifikasjon.consumer.TestUtils;
import no.nav.doknotifikasjon.exception.technical.KafkaTechnicalException;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.MottakerIdType;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.EmbededKafkaBroker;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.OVERSENDT_NOTIFIKASJON_PROCESSED;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ActiveProfiles("itestWeb")
public class Knot001ITest extends EmbededKafkaBroker {

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
	public void knot001ConsumerOnlySaveEpostWhenSmsAsPreferedKanalAndOnlyEpostIsValidKontaktInfo() {
		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjonWithPreferedKanalAsSms();

		this.stubGetKontaktInfoWithoutSmsInKontaktInfo();

		this.putMessageOnKafkaTopic(doknotifikasjon);

		verify(statusProducer).publishDoknotikfikasjonStatusOversendt(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
		);

		Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(doknotifikasjon.getBestillingsId());

		assertEquals(doknotifikasjon.getBestillerId(), notifikasjon.getBestillerId());
		assertEquals(doknotifikasjon.getBestillingsId(), notifikasjon.getBestillingsId());
		assertEquals(doknotifikasjon.getAntallRenotifikasjoner(), notifikasjon.getAntallRenotifikasjoner());
		assertEquals(doknotifikasjon.getRenotifikasjonIntervall(), notifikasjon.getRenotifikasjonIntervall());
		assertEquals(doknotifikasjon.getPrefererteKanaler().get(0).toString(), notifikasjon.getPrefererteKanaler());
		assertEquals(Status.OPPRETTET, notifikasjon.getStatus());

		assertEquals(doknotifikasjon.getFodselsnummer(), notifikasjon.getMottakerId());
		assertEquals(MottakerIdType.FNR, notifikasjon.getMottakerIdType());

		List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAll();

		assertEquals(1, notifikasjonDistribusjonList.size());

		NotifikasjonDistribusjon epost = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == Kanal.EPOST).findFirst().get();

		assertEquals(Kanal.EPOST, epost.getKanal());
		assertEquals(notifikasjon.getBestillingsId(), epost.getOpprettetAv());
		assertEquals(notifikasjon.getId(), epost.getNotifikasjon().getId());
		assertEquals(Status.OPPRETTET, epost.getStatus());
		assertEquals(doknotifikasjon.getEpostTekst(), epost.getTekst());
		assertEquals(doknotifikasjon.getTittel(), epost.getTittel());
	}


	@Test
	public void knot001ConsumerShouldRunSmoothlyWhenReceivingKafkaEventWithSmsAsPreferedKanal() {
		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjonWithPreferedKanalAsSms();
		this.stubGetKontaktInfo();

		this.putMessageOnKafkaTopic(doknotifikasjon);

		verify(statusProducer).publishDoknotikfikasjonStatusOversendt(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
		);

		Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(doknotifikasjon.getBestillingsId());

		assertEquals(doknotifikasjon.getBestillerId(), notifikasjon.getBestillerId());
		assertEquals(doknotifikasjon.getBestillingsId(), notifikasjon.getBestillingsId());
		assertEquals(doknotifikasjon.getAntallRenotifikasjoner(), notifikasjon.getAntallRenotifikasjoner());
		assertEquals(doknotifikasjon.getRenotifikasjonIntervall(), notifikasjon.getRenotifikasjonIntervall());
		assertEquals(doknotifikasjon.getPrefererteKanaler().get(0).toString(), notifikasjon.getPrefererteKanaler());
		assertEquals(Status.OPPRETTET, notifikasjon.getStatus());

		assertEquals(doknotifikasjon.getFodselsnummer(), notifikasjon.getMottakerId());
		assertEquals(MottakerIdType.FNR, notifikasjon.getMottakerIdType());

		List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAll();

		assertEquals(1, notifikasjonDistribusjonList.size());

		NotifikasjonDistribusjon sms = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == Kanal.SMS).findFirst().get();

		assertEquals(Kanal.SMS, sms.getKanal());
		assertEquals(notifikasjon.getBestillingsId(), sms.getOpprettetAv());
		assertEquals(notifikasjon.getId(), sms.getNotifikasjon().getId());
		assertEquals(Status.OPPRETTET, sms.getStatus());
		assertEquals(doknotifikasjon.getSmsTekst(), sms.getTekst());
		assertEquals(doknotifikasjon.getTittel(), sms.getTittel());
	}

	@Test
	public void knot001ConsumerShouldReceiveAndKafkaEventProcessWhenReceivingOneKafkaEvent() {
		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjon();
		this.stubGetKontaktInfo();

		this.putMessageOnKafkaTopic(doknotifikasjon);

		verify(statusProducer).publishDoknotikfikasjonStatusOversendt(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
		);

		Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(doknotifikasjon.getBestillingsId());

		assertEquals(doknotifikasjon.getBestillerId(), notifikasjon.getBestillerId());
		assertEquals(doknotifikasjon.getBestillingsId(), notifikasjon.getBestillingsId());
		assertEquals(doknotifikasjon.getAntallRenotifikasjoner(), notifikasjon.getAntallRenotifikasjoner());
		assertEquals(doknotifikasjon.getRenotifikasjonIntervall(), notifikasjon.getRenotifikasjonIntervall());
		assertEquals(doknotifikasjon.getPrefererteKanaler().get(0) + ", " + doknotifikasjon.getPrefererteKanaler().get(1), notifikasjon.getPrefererteKanaler());
		assertEquals(Status.OPPRETTET, notifikasjon.getStatus());

		assertEquals(doknotifikasjon.getFodselsnummer(), notifikasjon.getMottakerId());
		assertEquals(MottakerIdType.FNR, notifikasjon.getMottakerIdType());

		List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAll();

		assertEquals(2, notifikasjonDistribusjonList.size());

		NotifikasjonDistribusjon epost = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == Kanal.EPOST).findFirst().get();
		NotifikasjonDistribusjon sms = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == Kanal.SMS).findFirst().get();

		assertEquals(Kanal.EPOST, epost.getKanal());
		assertEquals(notifikasjon.getBestillingsId(), epost.getOpprettetAv());
		assertEquals(notifikasjon.getId(), epost.getNotifikasjon().getId());
		assertEquals(Status.OPPRETTET, epost.getStatus());
		assertEquals(doknotifikasjon.getEpostTekst(), epost.getTekst());
		assertEquals(doknotifikasjon.getTittel(), epost.getTittel());

		assertEquals(Kanal.SMS, sms.getKanal());
		assertEquals(notifikasjon.getBestillingsId(), sms.getOpprettetAv());
		assertEquals(notifikasjon.getId(), sms.getNotifikasjon().getId());
		assertEquals(Status.OPPRETTET, sms.getStatus());
		assertEquals(doknotifikasjon.getSmsTekst(), sms.getTekst());
		assertEquals(doknotifikasjon.getTittel(), sms.getTittel());
	}

	@Test
	public void shouldNotPersistInDatabaseWhenExceptionIsThrown() {
		Doknotifikasjon doknotifikasjon = TestUtils.createDoknotifikasjon();
		this.stubGetKontaktInfo();

		statusProducer.publishDoknotikfikasjonStatusOversendt(
				doknotifikasjon.getBestillingsId(),
				doknotifikasjon.getBestillerId(),
				OVERSENDT_NOTIFIKASJON_PROCESSED,
				null
		);

		doThrow(KafkaTechnicalException.class)
				.when(statusProducer)
				.publishDoknotikfikasjonStatusOversendt(doknotifikasjon.getBestillingsId(),
						doknotifikasjon.getBestillerId(),
						OVERSENDT_NOTIFIKASJON_PROCESSED,
						null);

		this.putMessageOnKafkaTopic(doknotifikasjon);

		Boolean isNotifikasjonPersistint = notifikasjonRepository.existsByBestillingsId(doknotifikasjon.getBestillerId());

		assertEquals(false, isNotifikasjonPersistint);
		assertEquals(0, notifikasjonDistribusjonRepository.findAll().size());

		verify(statusProducer, atLeast(1)).publishDoknotikfikasjonStatusOversendt(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
		);
	}


	private void putMessageOnKafkaTopic(Doknotifikasjon doknotifikasjon) {
		try {
			Long keyGenerator = System.currentTimeMillis();

			KafkaEventProducer.publish(
					KAFKA_TOPIC_DOK_NOTIFKASJON,
					doknotifikasjon,
					keyGenerator
			);

			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException exception) {
			fail();
		}
	}

	private void stubGetSecurityToken() {
		stubFor(get("/securitytoken?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("stsResponse_happy.json")));


	}

	private void stubGetKontaktInfo() {
		stubFor(get(urlEqualTo("/dkif/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false"))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("happy-responsebody.json")));

	}

	private void stubGetKontaktInfoWithoutSmsInKontaktInfo() {
		stubFor(get(urlEqualTo("/dkif/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false"))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("responsebodyWithoutSms.json")));

	}
}