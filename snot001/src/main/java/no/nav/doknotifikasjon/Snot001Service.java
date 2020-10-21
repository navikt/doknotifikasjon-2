package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.KafkaProducer.KafkaEventProducer;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST;
import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS;

@Slf4j
@Component
public class Snot001Service {

    private static final String SNOT001 = "SNOOT001";

    private final NotifikasjonRepository notifikasjonRepository;
    private final NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;
    private final KafkaEventProducer kafkaEventProducer;

    public Snot001Service(NotifikasjonRepository notifikasjonRepository, KafkaEventProducer kafkaEventProducer,
                          NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository) {
        this.notifikasjonRepository = notifikasjonRepository;
        this.kafkaEventProducer = kafkaEventProducer;
        this.notifikasjonDistribusjonRepository = notifikasjonDistribusjonRepository;
    }

    public void resendNotifikasjoner() {
        log.info("Starter Snot001 for å finne notifikasjoner som skal resendes.");

        List<Notifikasjon> notifikasjonList = notifikasjonRepository.findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoBefore(Status.OVERSENDT, 0, LocalDate.now());

        if (notifikasjonList.isEmpty()) {
            log.info("Ingen notifikasjoner ble funnet for resending. Avslutter snot001.");
            return;
        }

        List<NotifikasjonDistribusjon> notifikasjonDistribusjonList = notifikasjonDistribusjonRepository.findAllByNotifikasjonIn(notifikasjonList);

        for (NotifikasjonDistribusjon notifikasjonDistribusjon : notifikasjonDistribusjonList) {
            if (Kanal.SMS.equals(notifikasjonDistribusjon.getKanal())) {
                NotifikasjonDistribusjon newNotifikasjonDistribusjon = persistToDBWithKanal(notifikasjonDistribusjon, Kanal.SMS);
                publishHendelseOnTopic(KAFKA_TOPIC_DOK_NOTIFKASJON_SMS, newNotifikasjonDistribusjon.getId().toString());
            }
            if (Kanal.EPOST.equals(notifikasjonDistribusjon.getKanal())) {
                NotifikasjonDistribusjon newNotifikasjonDistribusjon = persistToDBWithKanal(notifikasjonDistribusjon, Kanal.EPOST);
                publishHendelseOnTopic(KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST, newNotifikasjonDistribusjon.getId().toString());
            }
            updateNotifikasjon(notifikasjonDistribusjon.getNotifikasjon());
        }
    }

    private void updateNotifikasjon(Notifikasjon notifikasjon) {
        notifikasjon.setAntallRenotifikasjoner(notifikasjon.getAntallRenotifikasjoner() - 1);
        notifikasjon.setNesteRenotifikasjonDato(notifikasjon.getAntallRenotifikasjoner() > 0 ? LocalDate.now().plusDays(notifikasjon.getRenotifikasjonIntervall()) : null);
        notifikasjon.setEndretAv(SNOT001);
        notifikasjon.setEndretDato(LocalDateTime.now());
    }

    private NotifikasjonDistribusjon persistToDBWithKanal(NotifikasjonDistribusjon notifikasjonDistribusjon, Kanal kanal) {
        NotifikasjonDistribusjon newNotifikasjonDistribusjon = NotifikasjonDistribusjon.builder()
                .notifikasjon(notifikasjonDistribusjon.getNotifikasjon())
                .status(Status.OPPRETTET)
                .kanal(kanal)
                .kontaktInfo(notifikasjonDistribusjon.getKontaktInfo())
                .tittel(notifikasjonDistribusjon.getTittel())
                .tekst("Påminnelse: " + notifikasjonDistribusjon.getTekst())
                .opprettetAv(SNOT001)
                .opprettetDato(LocalDateTime.now())
                .build();

        return notifikasjonDistribusjonRepository.save(newNotifikasjonDistribusjon);
    }

    private void publishHendelseOnTopic(String topic, String notifikasjonDistribusjonId) {

        DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(notifikasjonDistribusjonId);

        kafkaEventProducer.publish(
                topic,
                doknotifikasjonSms,
                System.currentTimeMillis()
        );
    }
}
