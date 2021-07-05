package no.nav.doknotifikasjon.consumer;


import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo;
import no.nav.doknotifikasjon.schemas.PrefererteKanal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.PRIVAT_DOK_NOTIFIKASJON_MED_KONTAKT_INFO;


@Slf4j
@RestController
@RequestMapping("/kafka6")
public class ProducerController {

	private final KafkaEventProducer publisher;

	ProducerController(KafkaEventProducer publisher) {
		this.publisher = publisher;
	}

	//This code should not be in prod!
	@GetMapping("/test")
	public void kafkaProduceMessage() {
		List<PrefererteKanal> preferteKanaler = List.of(PrefererteKanal.EPOST, PrefererteKanal.SMS);

		NotifikasjonMedkontaktInfo dokEksternNotifikasjon = new NotifikasjonMedkontaktInfo(
				LocalDateTime.now().toString(),
				LocalDateTime.now().toString(),
				"09097400366", // FNR er fra en testbrukker hos dolly,
				"telef",
				"epostadd",
				0,
				0,
				"TITTEL",
				"epostTekst",
				"smsTekst",
				preferteKanaler
		);

		publisher.publish(
				PRIVAT_DOK_NOTIFIKASJON_MED_KONTAKT_INFO,
				dokEksternNotifikasjon
		);
	}
}