package no.nav.doknotifikasjon.consumer;


import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp;
import no.nav.doknotifikasjon.schemas.PrefererteKanal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_STOPP;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON;


@Slf4j
@RestController
@RequestMapping("/kafka")
public class ProducerController {

    private final KafkaEventProducer publisher;

    ProducerController(KafkaEventProducer publisher) {
        this.publisher = publisher;
    }

    //TODO remove after testing
    @GetMapping("/test")
    public void kafkaProduceMessage() {
        List<PrefererteKanal> preferteKanaler = List.of(PrefererteKanal.EPOST, PrefererteKanal.SMS);

        Doknotifikasjon dokEksternNotifikasjon = new Doknotifikasjon(
                LocalDateTime.now().toString(),
                LocalDateTime.now().toString(),
                0,
                "09097400366", // FNR er fra en testbrukker hos dolly
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