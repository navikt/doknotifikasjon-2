package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonService;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_RESENDES;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;

@Slf4j
@Component
public class Snot002Service {

	private final NotifikasjonService notifikasjonService;
	private final NotifikasjonDistribusjonService notifikasjonDistribusjonService;
	private final KafkaStatusEventProducer kafkaStatusEventProducer;

	public Snot002Service(
			NotifikasjonService notifikasjonService,
			KafkaStatusEventProducer kafkaStatusEventProducer,
			NotifikasjonDistribusjonService notifikasjonDistribusjonService
	) {
		this.notifikasjonService = notifikasjonService;
		this.kafkaStatusEventProducer = kafkaStatusEventProducer;
		this.notifikasjonDistribusjonService = notifikasjonDistribusjonService;
	}

	public void oppdaterNotifikasjonStatus() {
		log.info("Snot002 leter etter notifikasjoner med status OVERSENDT eller OPPRETTET som skal oppdateres med status=FERDIGSTILT.");

		List<Notifikasjon> notifikasjonList = notifikasjonService.findAllWithStatusOpprettetOrOversendtAndNoRenotifikasjoner()
				.stream()
				.filter(this::checkIfLatestNotifikasjonDistribusjonHaveStatusFerdigstilt)
				.toList();

		if (notifikasjonList.isEmpty()) {
			log.info("Snot002 fant ingen notifikasjoner som skal oppdatere status til FERDIGSTILT. Avslutter Snot002.");
		} else {
			log.info("Snot002 fant antall={} notifikasjoner for oppdatering av status.", notifikasjonList.size());
			notifikasjonList.forEach(this::publishHendelseOnTopic);

			log.info("Snot002 er ferdig med å oppdatere status på antall={} notifikasjoner.", notifikasjonList.size());
		}
	}

	private boolean checkIfLatestNotifikasjonDistribusjonHaveStatusFerdigstilt(Notifikasjon notifikasjon) {
		Optional<NotifikasjonDistribusjon> sms = notifikasjonDistribusjonService.findFirstByNotifikasjonAndKanalAndEndretDatoIsNotNullOrderByEndretDatoDesc(notifikasjon, SMS);
		Optional<NotifikasjonDistribusjon> epost = notifikasjonDistribusjonService.findFirstByNotifikasjonAndKanalAndEndretDatoIsNotNullOrderByEndretDatoDesc(notifikasjon, EPOST);

		if (sms.isEmpty() && epost.isEmpty()) {
			return false;
		}

		return (sms.isEmpty() || FERDIGSTILT.equals(sms.get().getStatus())) && (epost.isEmpty() || FERDIGSTILT.equals(epost.get().getStatus()));
	}


	private void publishHendelseOnTopic(Notifikasjon notifikasjon) {
		kafkaStatusEventProducer.publishDoknotifikasjonStatusFerdigstilt(
				notifikasjon.getBestillingsId(),
				notifikasjon.getBestillerId(),
				FERDIGSTILT_RESENDES,
				null
		);
	}
}
