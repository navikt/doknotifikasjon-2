package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.KafkaProducer.KafkaDoknotifikasjonStatusProducer;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Component
public class Knot004Service {

	private final NotifikasjonRepository notifikasjonRepository;
	private final DoknotifikasjonStatusValidator doknotifikasjonStatusValidator;
	private final KafkaDoknotifikasjonStatusProducer kafkaDoknotifikasjonStatusProducer;

	public Knot004Service(NotifikasjonRepository notifikasjonRepository, DoknotifikasjonStatusValidator doknotifikasjonStatusValidator,
						  KafkaDoknotifikasjonStatusProducer kafkaDoknotifikasjonStatusProducer) {
		this.notifikasjonRepository = notifikasjonRepository;
		this.doknotifikasjonStatusValidator = doknotifikasjonStatusValidator;
		this.kafkaDoknotifikasjonStatusProducer = kafkaDoknotifikasjonStatusProducer;
	}

	public void shouldUpdateStatus(DoknotifikasjonStatusTo doknotifikasjonStatusTo) {
		log.info("Ny hendelse med bestillingId={} på kafka-topic dok-eksternnotifikasjon-status hentet av knot004.", doknotifikasjonStatusTo
				.getBestillingId());

		doknotifikasjonStatusValidator.shouldValidateInput(doknotifikasjonStatusTo);
		Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingId(doknotifikasjonStatusTo.getBestillingId());

		if (notifikasjon == null) {
			log.warn("Notifikasjon med bestillingId={} finnes ikke i notifikasjons databasen. Avslutter behandlingen. ", doknotifikasjonStatusTo.getBestillingId());
		} else if (doknotifikasjonStatusTo.getDistribusjonId() != null && isAllDistribusjonStatusEqualInputStatus(notifikasjon.getNotifikasjonDistribusjon(), doknotifikasjonStatusTo
				.getStatus())) {
			log.info("Alle distribusjoner knyttet til notifikasjon med bestillingId={} har status={}. Ny hendelse skrives til kafka-topic dok-eksternotifikasjon-status.", doknotifikasjonStatusTo
					.getBestillingId(), doknotifikasjonStatusTo.getStatus());

			kafkaDoknotifikasjonStatusProducer.publishDoknotifikasjonStatus(doknotifikasjonStatusTo
							.getBestillingId(), doknotifikasjonStatusTo.getBestillerId(),
					Status.valueOf(doknotifikasjonStatusTo.getStatus()), "notifikasjon er " + doknotifikasjonStatusTo
							.getStatus(), null);
		} else if (doknotifikasjonStatusTo.getDistribusjonId() == null) {
			if (Status.FERDIGSTILT.toString().equals(doknotifikasjonStatusTo.getStatus()) && notifikasjon.getAntallRenotifikasjoner() > 0) {
				log.info("En hendelse på kafka-topic dok-eksternnotifikasjon-status har status={} og notifikasjonen knyttet til " +
						"hendelsen har mer enn null antall renotifikasjoner. Behandlingen av hendelsen avsluttes. ", Status.FERDIGSTILT);
			} else {
				notifikasjon.setStatus(Status.valueOf(doknotifikasjonStatusTo.getStatus()));
				notifikasjon.setEndretAv(doknotifikasjonStatusTo.getBestillerId());
				notifikasjon.setEndretDato(LocalDateTime.now());
				notifikasjonRepository.save(notifikasjon);
			}
		}
	}

	private boolean isAllDistribusjonStatusEqualInputStatus(Set<NotifikasjonDistribusjon> notifikasjonDistribusjonsSet, String inputStatus) {
		for (NotifikasjonDistribusjon notifikasjonDistribusjon : notifikasjonDistribusjonsSet) {
			if (!inputStatus.equals(notifikasjonDistribusjon.getStatus().toString())) {
				return false;
			}
		}
		return true;
	}
}
