package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.repository.utils.AbstractKafkaBrokerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static no.nav.doknotifikasjon.TestUtils.createNotifikasjonDistribusjonWithNotifikasjonAndStatusAndKanal;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjonDistribusjonWithNotifikasjonSetAsFerdigstilt;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjonWithStatus;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjonWithStatusOversendt;
import static no.nav.doknotifikasjon.TestUtils.createNotifikasjonWithStatusOversendtAndAntallRenotifikasjoner;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_RESENDES;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.kodeverk.Status.OVERSENDT;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class Snot002ScheduledTest extends AbstractKafkaBrokerTest {

	@Autowired
	private NotifikasjonRepository notifikasjonRepository;

	@Autowired
	private NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	@Autowired
	private Snot002Scheduler scheduler;

	@MockBean
	private KafkaStatusEventProducer statusProducer;

	@BeforeEach
	public void setup() {
		notifikasjonDistribusjonRepository.deleteAll();
		notifikasjonRepository.deleteAll();
	}

	@Test
	void happyPath() {
		Notifikasjon notifikasjon = createNotifikasjonWithStatusOversendt(LocalDateTime.now());
		Set<NotifikasjonDistribusjon> notifikasjonDistribusjon = new HashSet<>();
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonSetAsFerdigstilt(notifikasjon, SMS));
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonSetAsFerdigstilt(notifikasjon, EPOST));

		notifikasjon.setNotifikasjonDistribusjon(notifikasjonDistribusjon);

		notifikasjonRepository.saveAndFlush(notifikasjon);
		scheduler.scheduledJob();

		verify(statusProducer).publishDoknotifikasjonStatusFerdigstilt(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), FERDIGSTILT_RESENDES, null
		);
	}

	@ParameterizedTest
	@EnumSource(value = Status.class, names = {"OPPRETTET", "OVERSENDT"})
	void happyPathWithOneNotifikasjonsDistribusjon(Status status) {
		Notifikasjon notifikasjon = createNotifikasjonWithStatus(LocalDateTime.now(), status);
		Set<NotifikasjonDistribusjon> notifikasjonDistribusjon = new HashSet<>();
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonSetAsFerdigstilt(notifikasjon, EPOST));

		notifikasjon.setNotifikasjonDistribusjon(notifikasjonDistribusjon);

		notifikasjonRepository.saveAndFlush(notifikasjon);
		scheduler.scheduledJob();

		verify(statusProducer).publishDoknotifikasjonStatusFerdigstilt(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), FERDIGSTILT_RESENDES, null
		);
	}

	@Test
	void shouldNotUpdateStatusWhenNotAllNotifikasjonDistribusjonHaveStatusFerdigstilt() {
		Notifikasjon notifikasjon = createNotifikasjonWithStatusOversendt(LocalDateTime.now());
		Set<NotifikasjonDistribusjon> notifikasjonDistribusjon = new HashSet<>();
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonSetAsFerdigstilt(notifikasjon, SMS));
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonAndStatusAndKanal(notifikasjon, OVERSENDT, EPOST));

		notifikasjon.setNotifikasjonDistribusjon(notifikasjonDistribusjon);

		notifikasjonRepository.saveAndFlush(notifikasjon);
		scheduler.scheduledJob();

		verify(statusProducer, times(0)).publishDoknotifikasjonStatusFerdigstilt(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), FERDIGSTILT_RESENDES, null
		);
	}

	@Test
	void shouldNotUpdateStatusWhenNoNotifikasjonDistribusjonIsFound() {
		Notifikasjon notifikasjon = createNotifikasjonWithStatusOversendt(LocalDateTime.now());

		notifikasjonRepository.saveAndFlush(notifikasjon);
		scheduler.scheduledJob();

		verify(statusProducer, times(0)).publishDoknotifikasjonStatusFerdigstilt(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), FERDIGSTILT_RESENDES, null
		);
	}

	@Test
	void shouldNotUpdateStatusWhenNotifikasjonHaveAntallRenotifikasjonGreaterThenZero() {
		Notifikasjon notifikasjon = createNotifikasjonWithStatusOversendtAndAntallRenotifikasjoner(LocalDateTime.now(), 2);
		Set<NotifikasjonDistribusjon> notifikasjonDistribusjon = new HashSet<>();
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonSetAsFerdigstilt(notifikasjon, SMS));
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonSetAsFerdigstilt(notifikasjon, EPOST));

		notifikasjon.setNotifikasjonDistribusjon(notifikasjonDistribusjon);

		notifikasjonRepository.saveAndFlush(notifikasjon);
		scheduler.scheduledJob();

		verify(statusProducer, times(0)).publishDoknotifikasjonStatusFerdigstilt(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), FERDIGSTILT_RESENDES, null
		);
	}

	@Test
	void shouldNotUpdateStatusWhenEndretDatoIsOutOfscope() {
		Notifikasjon notifikasjon = createNotifikasjonWithStatusOversendt(LocalDateTime.now().minusDays(60));
		Set<NotifikasjonDistribusjon> notifikasjonDistribusjon = new HashSet<>();
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonSetAsFerdigstilt(notifikasjon, SMS));
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonSetAsFerdigstilt(notifikasjon, EPOST));

		notifikasjon.setNotifikasjonDistribusjon(notifikasjonDistribusjon);

		notifikasjonRepository.saveAndFlush(notifikasjon);
		scheduler.scheduledJob();

		verify(statusProducer, times(0)).publishDoknotifikasjonStatusFerdigstilt(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), FERDIGSTILT_RESENDES, null
		);
	}

	@ParameterizedTest
	@EnumSource(value = Status.class, names = {"FEILET", "INFO"})
	void shouldNotUpdateStatusWhenNotifikasjonHaveStatusNotOpprettetOrOversendt(Status status) {
		Notifikasjon notifikasjon = createNotifikasjonWithStatus(LocalDateTime.now().minusDays(29), status);
		Set<NotifikasjonDistribusjon> notifikasjonDistribusjon = new HashSet<>();
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonSetAsFerdigstilt(notifikasjon, SMS));
		notifikasjonDistribusjon.add(createNotifikasjonDistribusjonWithNotifikasjonSetAsFerdigstilt(notifikasjon, EPOST));

		notifikasjon.setNotifikasjonDistribusjon(notifikasjonDistribusjon);

		notifikasjonRepository.saveAndFlush(notifikasjon);
		scheduler.scheduledJob();

		verify(statusProducer, times(0)).publishDoknotifikasjonStatusFerdigstilt(
				notifikasjon.getBestillingsId(), notifikasjon.getBestillerId(), FERDIGSTILT_RESENDES, null
		);
	}
}
