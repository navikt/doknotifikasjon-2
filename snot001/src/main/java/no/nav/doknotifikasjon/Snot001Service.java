package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;
import static no.nav.doknotifikasjon.kodeverk.Status.OVERSENDT;

@Slf4j
@Component
public class Snot001Service {

	private final NotifikasjonService notifikasjonService;
	private final Snot001NotifikasjonService snot001NotifikasjonService;
	private final KafkaEventProducer kafkaEventProducer;

	@Inject
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
		log.info("Starter Snot001 for å finne notifikasjoner som skal resendes.");

		List<Notifikasjon> notifikasjonList = notifikasjonService.findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoIsLessThanEqual(
				OVERSENDT,
				0,
				LocalDate.now()
		);

		if (notifikasjonList.isEmpty()) {
			log.info("Ingen notifikasjoner ble funnet for resending. Avslutter snot001.");
			return;
		}

		log.info("{} notifikasjoner ble funnet for resending i snot001. ", notifikasjonList.size());

		notifikasjonList.forEach(n -> {
					List<NotifikasjonDistribusjon> publishList = snot001NotifikasjonService.processNotifikasjon(n);
					publishList.forEach(nd ->
							this.publishHendelseOnTopic(
									nd.getId(),
									nd.getKanal(),
									n.getBestillingsId()
							)
					);
				}
		);
	}

	private void publishHendelseOnTopic(int notifikasjonDistribusjonId, Kanal kanal, String bestillingsId) {
		kafkaEventProducer.publishWithKey(
				kanal.equals(SMS) ? KAFKA_TOPIC_DOK_NOTIFKASJON_SMS : KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST,
				kanal.equals(SMS) ? new DoknotifikasjonSms(notifikasjonDistribusjonId) : new DoknotifikasjonEpost(notifikasjonDistribusjonId),
				bestillingsId
		);
	}
}
