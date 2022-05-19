package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_EPOST;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_SMS;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.kodeverk.Status.OVERSENDT;

@Slf4j
@Component
public class Snot001Service {

	private final NotifikasjonService notifikasjonService;
	private final Snot001NotifikasjonService snot001NotifikasjonService;
	private final KafkaEventProducer kafkaEventProducer;

	@Autowired
	public Snot001Service(
			NotifikasjonService notifikasjonService,
			KafkaEventProducer kafkaEventProducer,
			Snot001NotifikasjonService snot001NotifikasjonService
	) {
		this.notifikasjonService = notifikasjonService;
		this.kafkaEventProducer = kafkaEventProducer;
		this.snot001NotifikasjonService = snot001NotifikasjonService;
	}

	public void resendNotifikasjoner() {
		log.info("Snot001 starter for å finne notifikasjoner som skal resendes.");

		List<Notifikasjon> notifikasjonList = notifikasjonService.findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoIsLessThanEqual(
				OVERSENDT,
				0,
				LocalDate.now()
		);

		if (notifikasjonList.isEmpty()) {
			log.info("Snot001 fant ingen notifikasjoner for resending. Avslutter.");
			return;
		}

		log.info("Snot001 fant antall={} notifikasjoner for resending.", notifikasjonList.size());

		notifikasjonList.forEach(notifikasjon -> {
					List<NotifikasjonDistribusjon> publishList = snot001NotifikasjonService.processNotifikasjon(notifikasjon);
					publishList.forEach(notifikasjonDistribusjon ->
							this.publishHendelseOnTopic(
									notifikasjonDistribusjon.getId(),
									notifikasjonDistribusjon.getKanal(),
									notifikasjon.getBestillingsId()
							)
					);
				}
		);
	}

	private void publishHendelseOnTopic(int notifikasjonDistribusjonId, Kanal kanal, String bestillingsId) {
		if (kanal == SMS) {
			log.info("Snot001 oppretter hendelse til topic={}, kanal={} for bestillingsId={}", KAFKA_TOPIC_DOK_NOTIFIKASJON_SMS, kanal, bestillingsId);
			kafkaEventProducer.publishWithKey(KAFKA_TOPIC_DOK_NOTIFIKASJON_SMS, new DoknotifikasjonSms(notifikasjonDistribusjonId), bestillingsId);
		} else if (kanal == EPOST) {
			log.info("Snot001 oppretter hendelse til topic={}, kanal={} for bestillingsId={}", KAFKA_TOPIC_DOK_NOTIFIKASJON_EPOST, kanal, bestillingsId);
			kafkaEventProducer.publishWithKey(KAFKA_TOPIC_DOK_NOTIFIKASJON_EPOST, new DoknotifikasjonEpost(notifikasjonDistribusjonId), bestillingsId);
		} else {
			log.error("Snot001 fant ugyldig kanal={} for bestillingsId={}, notifikasjonDistribusjonId={}. Fortsetter behandling.",
					kanal, bestillingsId, kanal);
		}
	}
}
