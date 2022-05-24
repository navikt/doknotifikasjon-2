package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistrubisjonService;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_RESENDES;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static no.nav.doknotifikasjon.kodeverk.Status.OVERSENDT;

@Slf4j
@Component
public class Snot002Service {

	private final NotifikasjonService notifikasjonService;
	private final NotifikasjonDistrubisjonService notifikasjonDistribusjonService;
	private final KafkaStatusEventProducer kafkaStatusEventProducer;

	private static final int AMOUNT_OF_DAYS_IN_SCOPE = 30;

	@Autowired
	public Snot002Service(
			NotifikasjonService notifikasjonService,
			KafkaStatusEventProducer kafkaStatusEventProducer,
			NotifikasjonDistrubisjonService notifikasjonDistribusjonService
	) {
		this.notifikasjonService = notifikasjonService;
		this.kafkaStatusEventProducer = kafkaStatusEventProducer;
		this.notifikasjonDistribusjonService = notifikasjonDistribusjonService;
	}

	public void resendNotifikasjoner() {
		log.info("Snot002 starter for å finne notifikasjoner som  har status={} og skal oppdateres med status={}.", OVERSENDT, FERDIGSTILT);
		LocalDateTime endretDato = LocalDateTime.now().minusDays(AMOUNT_OF_DAYS_IN_SCOPE);

		List<Notifikasjon> notifikasjonList = notifikasjonService.findAllByStatusAndEndretDatoIsGreaterThanEqualWithNoAntallRenotifikasjoner(OVERSENDT, endretDato)
				.stream()
				.filter(this::checkIfLatestNotifikasjonDistrubusjonHaveStatusFerdigstilt).toList();

		if (notifikasjonList.isEmpty()) {
			log.info("Snot002 fant ingen notifikasjoner for oppdatering av status. Avslutter Snot002.");
			return;
		}

		log.info("Snot002 fant antall={} notifikasjoner for oppdatering av status.", notifikasjonList.size());

		notifikasjonList.forEach(this::publishHendelseOnTopic);
	}

	private boolean checkIfLatestNotifikasjonDistrubusjonHaveStatusFerdigstilt(Notifikasjon notifikasjon) {
		Optional<NotifikasjonDistribusjon> sms = notifikasjonDistribusjonService.findFirstByNotifikasjonAndKanalAndEndretDatoIsNotNullOrderByEndretDatoDesc(notifikasjon, Kanal.SMS);
		Optional<NotifikasjonDistribusjon> epost = notifikasjonDistribusjonService.findFirstByNotifikasjonAndKanalAndEndretDatoIsNotNullOrderByEndretDatoDesc(notifikasjon, Kanal.EPOST);

		if (sms.isEmpty() && epost.isEmpty()) {
			return false;
		}

		return (sms.isEmpty() || FERDIGSTILT.equals(sms.get().getStatus())) && (epost.isEmpty() || FERDIGSTILT.equals(epost.get().getStatus()));
	}


	private void publishHendelseOnTopic(Notifikasjon notifikasjon) {
		kafkaStatusEventProducer.publishDoknotikfikasjonStatusFerdigstilt(
				notifikasjon.getBestillingsId(),
				notifikasjon.getBestillerId(),
				FERDIGSTILT_RESENDES,
				null
		);
	}
}
