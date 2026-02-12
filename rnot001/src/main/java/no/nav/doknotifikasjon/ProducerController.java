package no.nav.doknotifikasjon;


import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo;
import no.nav.doknotifikasjon.schemas.PrefererteKanal;
import no.nav.security.token.support.core.api.Unprotected;import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.PRIVAT_DOK_NOTIFIKASJON_MED_KONTAKT_INFO;

/*
	RestController Brukes for å teste doknotifikasjon med å produsere records til knot001 og knot006. NB Denne koden skal kun brukes
	til å teste mot dev og lokalt milø, må ikke være prod!
* */
@Slf4j
@Unprotected
@RestController
@RequestMapping("/rest/v1//kafkatest")
public class ProducerController {

	private final KafkaEventProducer publisher;

	ProducerController(KafkaEventProducer publisher) {
		this.publisher = publisher;
	}

	/*
		kafkaProduceMessage Brukes for å teste Knot006 med å produces a record to topic privat-dok-notifikasjon-med-kontakt-info.
	* */
	@GetMapping(path = "/notifikasjonMedKontaktInfo", produces = "text/plain")
	public String kafkaProduceMessage(@Param("kanal") String kanal, @Param("phone") String phone, @Param("email") String email, @Param("type") String type) {
		List<PrefererteKanal> preferteKanaler = kanal == null ? List.of(PrefererteKanal.EPOST, PrefererteKanal.SMS) : List.of(PrefererteKanal.valueOf(kanal.toUpperCase()));
		var epostTekst = "epostTekst" + LocalDateTime.now();
		if ("HTML".equalsIgnoreCase(type)) {
			epostTekst = "<!doctype html><html><body><h1>Varsel test</h1><p>Du har fått et testvarsel</p><p><strong>Gratulerer!</strong></p></body></html>";
		}
		String bestillingsId = UUID.randomUUID().toString();
		var tlf = phone == null ? "" : phone;
		var epost = email == null ? "" : email;

		NotifikasjonMedkontaktInfo dokEksternNotifikasjon = new NotifikasjonMedkontaktInfo(
				bestillingsId,
				LocalDateTime.now().toString(),
				"09097400366", // FNR er fra en testbrukker hos dolly,
				tlf,
				epost,
				0,
				0,
				"Testvarsel: test-tittel",
				epostTekst,
				"smsTekst" + LocalDateTime.now(),
				preferteKanaler
		);

		publisher.publish(
			PRIVAT_DOK_NOTIFIKASJON_MED_KONTAKT_INFO,
				dokEksternNotifikasjon
		);
		return bestillingsId;
	}

/*
	kafkaProduceMessage Brukes for å teste Knot001 med å produces a record to topic privat-dok-notifikasjon.
* */
	@GetMapping("/notifikasjon")
	public void publishDoknotifikasjon() {
		List<PrefererteKanal> preferteKanaler = List.of(PrefererteKanal.EPOST, PrefererteKanal.SMS);

		Doknotifikasjon dokEksternNotifikasjon = new Doknotifikasjon(
				LocalDateTime.now().toString(),
				LocalDateTime.now().toString(),
				0,
				"09097400366", // FNR er fra en testbrukker hos dolly
				0,
				0,
				"TITTEL",
				"epostTekst",
				"smsTekst",
				preferteKanaler
		);

		publisher.publish(
				KAFKA_TOPIC_DOK_NOTIFIKASJON,
				dokEksternNotifikasjon
		);
	}
}
