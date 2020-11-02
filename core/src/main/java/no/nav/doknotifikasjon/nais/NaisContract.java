package no.nav.doknotifikasjon.nais;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.schemas.PrefererteKanal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON;

@Slf4j
@RestController
public class NaisContract {

	private static final String APPLICATION_ALIVE = "Application is alive!";
	private static final String APPLICATION_READY = "Application is ready for traffic!";

	private final KafkaEventProducer publisher;// todo remove

	private AtomicInteger appStatus = new AtomicInteger(1);

	@Inject
	public NaisContract(MeterRegistry registry, KafkaEventProducer publisher) {
		this.publisher = publisher;// todo remove
		Gauge.builder("dok_app_is_ready", appStatus, AtomicInteger::get).register(registry);
	}

	@GetMapping("/isAlive")
	public String isAlive() {
		return APPLICATION_ALIVE;
	}

	@RequestMapping(value = "/isReady", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity isReady() {
		appStatus.set(1);

		return new ResponseEntity<>(APPLICATION_READY, HttpStatus.OK);
	}

	//TODO remove after testing
	@GetMapping("/isAlive2")
	public void kafkaProduceMessage() {
		List<PrefererteKanal> preferteKanaler = List.of(PrefererteKanal.EPOST, PrefererteKanal.SMS);

		Doknotifikasjon dokEksternNotifikasjon = new Doknotifikasjon(
				LocalDateTime.now().toString(),
				LocalDateTime.now().toString(),
				"08048422250", // FNR er fra en testbrukker hos dolly
				0,
				0,
				"tittel",
				"epostTekst",
				"smsTekst",
				preferteKanaler
		);

		publisher.publish(
				KAFKA_TOPIC_DOK_NOTIFKASJON,
				dokEksternNotifikasjon
		);
	}
}