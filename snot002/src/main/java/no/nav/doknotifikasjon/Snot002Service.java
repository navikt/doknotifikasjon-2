package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistrubisjonService;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_RESENDES;
import static no.nav.doknotifikasjon.kodeverk.Status.*;

@Slf4j
@Component
public class Snot002Service {

	private final NotifikasjonService notifikasjonService;
	private final NotifikasjonDistrubisjonService notifikasjonDistrubisjonService;
	private final KafkaStatusEventProducer kafkaStatusEventProducer;

	private final int AMOUNT_OF_DAYS_IN_SCOPE = 30;

	@Inject
	public Snot002Service(
			NotifikasjonService notifikasjonService,
			KafkaStatusEventProducer kafkaStatusEventProducer,
			NotifikasjonDistrubisjonService notifikasjonDistrubisjonService
	) {
		this.notifikasjonService = notifikasjonService;
		this.kafkaStatusEventProducer = kafkaStatusEventProducer;
		this.notifikasjonDistrubisjonService = notifikasjonDistrubisjonService;
	}

	public void resendNotifikasjoner() {
		log.info("Starter Snot002 for å finne notifikasjoner som  har status={} og skal oppdateres med status={}.", OVERSENDT, FERDIGSTILT);
		LocalDateTime endretDato = LocalDateTime.now().minusDays(AMOUNT_OF_DAYS_IN_SCOPE);

		List<Notifikasjon> notifikasjonList = notifikasjonService.findAllByStatusAndEndretDatoIsGreaterThanEqualWithNoAntallRenotifikasjoner(OVERSENDT, endretDato)
				.stream()
				.filter(this::checkIfLatestNotifikasjonDistrubusjonHaveStatusFerdigstilt)
				.collect(Collectors.toList());

		if (notifikasjonList.isEmpty()) {
			log.info("Ingen notifikasjoner ble funnet for oppdatering av status. Avslutter snot002.");
			return;
		}

		log.info("{} notifikasjoner ble funnet for oppdatering av status i snot002.", notifikasjonList.size());

		notifikasjonList.forEach(this::publishHendelseOnTopic);
	}

	private boolean checkIfLatestNotifikasjonDistrubusjonHaveStatusFerdigstilt(Notifikasjon notifikasjon) {
		Optional<NotifikasjonDistribusjon> sms = notifikasjonDistrubisjonService.findFirstByNotifikasjonAndKanalAndEndretDatoIsNotNullOrderByEndretDatoDesc(notifikasjon, Kanal.SMS);
		Optional<NotifikasjonDistribusjon> epost = notifikasjonDistrubisjonService.findFirstByNotifikasjonAndKanalAndEndretDatoIsNotNullOrderByEndretDatoDesc(notifikasjon, Kanal.EPOST);

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
