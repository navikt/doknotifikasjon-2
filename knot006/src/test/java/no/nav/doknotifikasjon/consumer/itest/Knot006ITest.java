package no.nav.doknotifikasjon.consumer.itest;

import no.nav.doknotifikasjon.consumer.TestUtils;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.AbstractKafkaBrokerTest;
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.OVERSENDT_NOTIFIKASJON_PROCESSED;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.PRIVAT_DOK_NOTIFIKASJON_MED_KONTAKT_INFO;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.kodeverk.MottakerIdType.FNR;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ActiveProfiles("wiremock")
class Knot006ITest extends AbstractKafkaBrokerTest {

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
	}

	@Test
	void shouldCreateSMSWhenDoknotifikasjonPreferedKanalIsEpostAndInvalidEpost() {
		NotifikasjonMedkontaktInfo notifikasjonMedkontaktInfo = TestUtils.createDoknotifikasjonWithPreferedKanalAndEpostIsnull();

		this.putMessageOnKafkaTopic(notifikasjonMedkontaktInfo);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotifikasjonStatusOversendt(
					notifikasjonMedkontaktInfo.getBestillingsId(), notifikasjonMedkontaktInfo.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
			);

			Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(notifikasjonMedkontaktInfo.getBestillingsId()).orElse(null);

			assertNotNull(notifikasjon);
			assertEquals(notifikasjonMedkontaktInfo.getBestillerId(), notifikasjon.getBestillerId());
			assertEquals(notifikasjonMedkontaktInfo.getBestillingsId(), notifikasjon.getBestillingsId());
			assertEquals(notifikasjonMedkontaktInfo.getAntallRenotifikasjoner(), notifikasjon.getAntallRenotifikasjoner());
			assertEquals(notifikasjonMedkontaktInfo.getRenotifikasjonIntervall(), notifikasjon.getRenotifikasjonIntervall());
			assertEquals(notifikasjonMedkontaktInfo.getPrefererteKanaler().getFirst().toString(), notifikasjon.getPrefererteKanaler());
			assertEquals(OPPRETTET, notifikasjon.getStatus());

			assertEquals(notifikasjonMedkontaktInfo.getFodselsnummer(), notifikasjon.getMottakerId());
			assertEquals(FNR, notifikasjon.getMottakerIdType());

			List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAll();

			assertEquals(1, notifikasjonDistribusjonList.size());

			NotifikasjonDistribusjon sms = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == SMS).findFirst().get();

			assertEquals(SMS, sms.getKanal());
			assertEquals(notifikasjon.getBestillingsId(), sms.getOpprettetAv());
			assertEquals(notifikasjon.getId(), sms.getNotifikasjon().getId());
			assertEquals(OPPRETTET, sms.getStatus());
			assertEquals(notifikasjonMedkontaktInfo.getSmsTekst(), sms.getTekst());
			assertEquals(notifikasjonMedkontaktInfo.getTittel(), sms.getTittel());
		});
	}

	@Test
	void shouldCreateEpostWhenDoknotifikasjonIsWithoutPreferedKanal() {
		NotifikasjonMedkontaktInfo notifikasjonMedkontaktInfo = TestUtils.createDoknotifikasjonWithPreferedKanalAsEpost();

		this.putMessageOnKafkaTopic(notifikasjonMedkontaktInfo);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotifikasjonStatusOversendt(
					notifikasjonMedkontaktInfo.getBestillingsId(), notifikasjonMedkontaktInfo.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
			);

			Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(notifikasjonMedkontaktInfo.getBestillingsId()).orElse(null);

			assertNotNull(notifikasjon);
			assertEquals(notifikasjonMedkontaktInfo.getBestillerId(), notifikasjon.getBestillerId());
			assertEquals(notifikasjonMedkontaktInfo.getBestillingsId(), notifikasjon.getBestillingsId());
			assertEquals(notifikasjonMedkontaktInfo.getAntallRenotifikasjoner(), notifikasjon.getAntallRenotifikasjoner());
			assertEquals(notifikasjonMedkontaktInfo.getRenotifikasjonIntervall(), notifikasjon.getRenotifikasjonIntervall());
			assertEquals(notifikasjonMedkontaktInfo.getPrefererteKanaler().getFirst().toString(), notifikasjon.getPrefererteKanaler());
			assertEquals(OPPRETTET, notifikasjon.getStatus());

			assertEquals(notifikasjonMedkontaktInfo.getFodselsnummer(), notifikasjon.getMottakerId());
			assertEquals(FNR, notifikasjon.getMottakerIdType());

			List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAll();

			assertEquals(1, notifikasjonDistribusjonList.size());

			NotifikasjonDistribusjon epost = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == EPOST).findFirst().get();

			assertEquals(EPOST, epost.getKanal());
			assertEquals(notifikasjon.getBestillingsId(), epost.getOpprettetAv());
			assertEquals(notifikasjon.getId(), epost.getNotifikasjon().getId());
			assertEquals(OPPRETTET, epost.getStatus());
			assertEquals(notifikasjonMedkontaktInfo.getEpostTekst(), epost.getTekst());
			assertEquals(notifikasjonMedkontaktInfo.getTittel(), epost.getTittel());
		});
	}


	@Test
	void shouldOnlySaveEpostWhenEpostIsPreferedKanal() {
		NotifikasjonMedkontaktInfo notifikasjonMedkontaktInfo = TestUtils.createDoknotifikasjonWithPreferedKanalAsEpost();

		this.putMessageOnKafkaTopic(notifikasjonMedkontaktInfo);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotifikasjonStatusOversendt(
					notifikasjonMedkontaktInfo.getBestillingsId(), notifikasjonMedkontaktInfo.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
			);

			Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(notifikasjonMedkontaktInfo.getBestillingsId()).orElse(null);

			assertNotNull(notifikasjon);
			assertEquals(notifikasjonMedkontaktInfo.getBestillerId(), notifikasjon.getBestillerId());
			assertEquals(notifikasjonMedkontaktInfo.getBestillingsId(), notifikasjon.getBestillingsId());
			assertEquals(notifikasjonMedkontaktInfo.getAntallRenotifikasjoner(), notifikasjon.getAntallRenotifikasjoner());
			assertEquals(notifikasjonMedkontaktInfo.getRenotifikasjonIntervall(), notifikasjon.getRenotifikasjonIntervall());
			assertEquals(notifikasjonMedkontaktInfo.getPrefererteKanaler().getFirst().toString(), notifikasjon.getPrefererteKanaler());
			assertEquals(OPPRETTET, notifikasjon.getStatus());

			assertEquals(notifikasjonMedkontaktInfo.getFodselsnummer(), notifikasjon.getMottakerId());
			assertEquals(FNR, notifikasjon.getMottakerIdType());

			List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAll();

			assertEquals(1, notifikasjonDistribusjonList.size());

			NotifikasjonDistribusjon epost = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == EPOST).findFirst().get();

			assertEquals(EPOST, epost.getKanal());
			assertEquals(notifikasjon.getBestillingsId(), epost.getOpprettetAv());
			assertEquals(notifikasjon.getId(), epost.getNotifikasjon().getId());
			assertEquals(OPPRETTET, epost.getStatus());
			assertEquals(notifikasjonMedkontaktInfo.getEpostTekst(), epost.getTekst());
			assertEquals(notifikasjonMedkontaktInfo.getTittel(), epost.getTittel());
		});
	}

	@Test
	void shouldRunWhenReceivingKafkaEventWithSmsAsPreferedKanal() {
		NotifikasjonMedkontaktInfo notifikasjonMedkontaktInfo = TestUtils.createDoknotifikasjonWithPreferedKanalAsSms();

		this.putMessageOnKafkaTopic(notifikasjonMedkontaktInfo);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotifikasjonStatusOversendt(
					notifikasjonMedkontaktInfo.getBestillingsId(), notifikasjonMedkontaktInfo.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
			);

			Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(notifikasjonMedkontaktInfo.getBestillingsId()).orElse(null);

			assertNotNull(notifikasjon);
			assertEquals(notifikasjonMedkontaktInfo.getBestillerId(), notifikasjon.getBestillerId());
			assertEquals(notifikasjonMedkontaktInfo.getBestillingsId(), notifikasjon.getBestillingsId());
			assertEquals(notifikasjonMedkontaktInfo.getAntallRenotifikasjoner(), notifikasjon.getAntallRenotifikasjoner());
			assertEquals(notifikasjonMedkontaktInfo.getRenotifikasjonIntervall(), notifikasjon.getRenotifikasjonIntervall());
			assertEquals(notifikasjonMedkontaktInfo.getPrefererteKanaler().getFirst().toString(), notifikasjon.getPrefererteKanaler());
			assertEquals(OPPRETTET, notifikasjon.getStatus());

			assertEquals(notifikasjonMedkontaktInfo.getFodselsnummer(), notifikasjon.getMottakerId());
			assertEquals(FNR, notifikasjon.getMottakerIdType());

			List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAll();

			assertEquals(1, notifikasjonDistribusjonList.size());

			NotifikasjonDistribusjon sms = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == SMS).findFirst().get();

			assertEquals(SMS, sms.getKanal());
			assertEquals(notifikasjon.getBestillingsId(), sms.getOpprettetAv());
			assertEquals(notifikasjon.getId(), sms.getNotifikasjon().getId());
			assertEquals(OPPRETTET, sms.getStatus());
			assertEquals(notifikasjonMedkontaktInfo.getSmsTekst(), sms.getTekst());
			assertEquals(notifikasjonMedkontaktInfo.getTittel(), sms.getTittel());
		});
	}

	@Test
	void shouldCreateNotifikasjonDistribusjonForBothSmsAndEpost() {
		NotifikasjonMedkontaktInfo notifikasjonMedkontaktInfo = TestUtils.createNotifikasjon();

		this.putMessageOnKafkaTopic(notifikasjonMedkontaktInfo);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(statusProducer).publishDoknotifikasjonStatusOversendt(
					notifikasjonMedkontaktInfo.getBestillingsId(), notifikasjonMedkontaktInfo.getBestillerId(), OVERSENDT_NOTIFIKASJON_PROCESSED, null
			);

			Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(notifikasjonMedkontaktInfo.getBestillingsId()).orElse(null);

			assertNotNull(notifikasjon);

			List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAll();
			assertEquals(notifikasjonMedkontaktInfo.getBestillerId(), notifikasjon.getBestillerId());
			assertEquals(notifikasjonMedkontaktInfo.getBestillingsId(), notifikasjon.getBestillingsId());
			assertEquals(notifikasjonMedkontaktInfo.getAntallRenotifikasjoner(), notifikasjon.getAntallRenotifikasjoner());
			assertEquals(notifikasjonMedkontaktInfo.getRenotifikasjonIntervall(), notifikasjon.getRenotifikasjonIntervall());
			assertEquals(notifikasjonMedkontaktInfo.getPrefererteKanaler().get(0) + ", " + notifikasjonMedkontaktInfo.getPrefererteKanaler().get(1), notifikasjon.getPrefererteKanaler());
			assertEquals(OPPRETTET, notifikasjon.getStatus());

			assertEquals(notifikasjonMedkontaktInfo.getFodselsnummer(), notifikasjon.getMottakerId());
			assertEquals(FNR, notifikasjon.getMottakerIdType());


			assertEquals(2, notifikasjonDistribusjonList.size());

			NotifikasjonDistribusjon epost = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == EPOST).findFirst().get();
			NotifikasjonDistribusjon sms = notifikasjonDistribusjonList.stream().filter(s -> s.getKanal() == SMS).findFirst().get();

			assertEquals(EPOST, epost.getKanal());
			assertEquals(notifikasjon.getBestillingsId(), epost.getOpprettetAv());
			assertEquals(notifikasjon.getId(), epost.getNotifikasjon().getId());
			assertEquals(OPPRETTET, epost.getStatus());
			assertEquals(notifikasjonMedkontaktInfo.getEpostTekst(), epost.getTekst());
			assertEquals(notifikasjonMedkontaktInfo.getTittel(), epost.getTittel());

			assertEquals(SMS, sms.getKanal());
			assertEquals(notifikasjon.getBestillingsId(), sms.getOpprettetAv());
			assertEquals(notifikasjon.getId(), sms.getNotifikasjon().getId());
			assertEquals(OPPRETTET, sms.getStatus());
			assertEquals(notifikasjonMedkontaktInfo.getSmsTekst(), sms.getTekst());
			assertEquals(notifikasjonMedkontaktInfo.getTittel(), sms.getTittel());
		});
	}


	private void putMessageOnKafkaTopic(NotifikasjonMedkontaktInfo notifikasjon) {
		KafkaEventProducer.publish(
				PRIVAT_DOK_NOTIFIKASJON_MED_KONTAKT_INFO,
				notifikasjon
		);
	}

}