package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS;

@Slf4j
@Component
public class Snot001Service {

    private static final String SNOT001 = "SNOT001";

    private final NotifikasjonRepository notifikasjonRepository;
    private final NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;
    private final KafkaEventProducer kafkaEventProducer;

    @Inject
    public Snot001Service(NotifikasjonRepository notifikasjonRepository, KafkaEventProducer kafkaEventProducer,
                          NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository) {
        this.notifikasjonRepository = notifikasjonRepository;
        this.kafkaEventProducer = kafkaEventProducer;
        this.notifikasjonDistribusjonRepository = notifikasjonDistribusjonRepository;
    }

    public void resendNotifikasjoner() {
        log.info("Starter Snot001 for å finne notifikasjoner som skal resendes.");

        List<Notifikasjon> notifikasjonList = notifikasjonRepository.findAllByStatusAndAntallRenotifikasjonerGreaterThanAndNesteRenotifikasjonDatoIsLessThanEqual(Status.OVERSENDT, 0, LocalDate.now());

        if (notifikasjonList.isEmpty()) {
            log.info("Ingen notifikasjoner ble funnet for resending. Avslutter snot001.");
            return;
        }

        log.info("{} notifikasjoner ble funnet for resending i snot001. ", notifikasjonList.size());

        notifikasjonList.forEach(n -> {
            if (n.getNotifikasjonDistribusjon().isEmpty()) {
                log.error("Notifikasjon med id {} hadde ingen notifikasjonDistrubisjon", n.getId());
                return;
            }

            Optional<NotifikasjonDistribusjon> sms = n.getNotifikasjonDistribusjon().stream().filter(nd -> Kanal.SMS.equals(nd.getKanal())).findFirst();
            Optional<NotifikasjonDistribusjon> epost = n.getNotifikasjonDistribusjon().stream().filter(nd -> Kanal.EPOST.equals(nd.getKanal())).findFirst();

            if (sms.isPresent()) {
                NotifikasjonDistribusjon newNotifikasjonDistribusjon = persistToDBWithKanal(sms.get(), Kanal.SMS);
                publishHendelseOnTopic(KAFKA_TOPIC_DOK_NOTIFKASJON_SMS, newNotifikasjonDistribusjon.getId(), Kanal.SMS);
            }

            if (epost.isPresent()) {
                NotifikasjonDistribusjon newNotifikasjonDistribusjon = persistToDBWithKanal(epost.get(), Kanal.EPOST);
                publishHendelseOnTopic(KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST, newNotifikasjonDistribusjon.getId(), Kanal.EPOST);
            }

            updateNotifikasjon(n);
        });
    }

    private void updateNotifikasjon(Notifikasjon notifikasjon) {
        log.info("Snot001 oppdaterer notifikasjon med bestillingsId={}", notifikasjon.getBestillingsId());

        notifikasjon.setAntallRenotifikasjoner(notifikasjon.getAntallRenotifikasjoner() - 1);
        notifikasjon.setNesteRenotifikasjonDato(notifikasjon.getAntallRenotifikasjoner() > 0 ? LocalDate.now().plusDays(notifikasjon.getRenotifikasjonIntervall()) : null);
        notifikasjon.setEndretAv(SNOT001);
        notifikasjon.setEndretDato(LocalDateTime.now());
    }

    private NotifikasjonDistribusjon persistToDBWithKanal(NotifikasjonDistribusjon notifikasjonDistribusjon, Kanal kanal) {
        log.info("Snot001 oppretter ny notifikasjonDistribusjon med kanal={} for notifikasjon med bestillingdId={}", kanal.toString(), notifikasjonDistribusjon.getNotifikasjon().getBestillingsId());

        String text = notifikasjonDistribusjon.getTekst().startsWith("Påminnelse: ") ?
                notifikasjonDistribusjon.getTekst() : "Påminnelse: " + notifikasjonDistribusjon.getTekst();

        NotifikasjonDistribusjon newNotifikasjonDistribusjon = NotifikasjonDistribusjon.builder()
                .notifikasjon(notifikasjonDistribusjon.getNotifikasjon())
                .status(Status.OPPRETTET)
                .kanal(kanal)
                .kontaktInfo(notifikasjonDistribusjon.getKontaktInfo())
                .tittel(notifikasjonDistribusjon.getTittel())
                .tekst(text)
                .opprettetAv(SNOT001)
                .opprettetDato(LocalDateTime.now())
                .build();
        return notifikasjonDistribusjonRepository.save(newNotifikasjonDistribusjon);
    }

    private void publishHendelseOnTopic(String topic, int notifikasjonDistribusjonId, Kanal kanal) {
        Object doknotifikasjon;

        if (kanal.equals(Kanal.SMS)) {
            doknotifikasjon = new DoknotifikasjonSms(notifikasjonDistribusjonId);
        } else {
            doknotifikasjon = new DoknotifikasjonEpost(notifikasjonDistribusjonId);
        }

        kafkaEventProducer.publish(
                topic,
                doknotifikasjon
        );
    }
}
