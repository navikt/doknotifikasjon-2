package no.nav.doknotifikasjon.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.KafkaProducer.KafkaDoknotifikasjonStatusProducer;
import no.nav.doknotifikasjon.KafkaProducer.KafkaEventProducer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.MottakerIdType;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import no.nav.doknotifikasjon.service.NotifikasjonDistrbusjonService;
import no.nav.doknotifikasjon.service.NotifikasjonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static no.nav.doknotifikasjon.KafkaProducer.DoknotifikasjonStatusMessage.FEILET_ALREADY_EXIST_IN_DATABASE;
import static no.nav.doknotifikasjon.KafkaProducer.DoknotifikasjonStatusMessage.FEILET_USER_DP_NOT_HAVE_VALID_CONTACT_INFORMATION;
import static no.nav.doknotifikasjon.KafkaProducer.DoknotifikasjonStatusMessage.FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET;
import static no.nav.doknotifikasjon.KafkaProducer.DoknotifikasjonStatusMessage.FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT;
import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST;
import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS;

@Slf4j
@Component
public class DoknotifikasjonService {

    @Autowired
    KafkaDoknotifikasjonStatusProducer StatusProducer;

    @Autowired
    NotifikasjonService notifikasjonService;

    @Autowired
    NotifikasjonDistrbusjonService notifikasjonDistrbusjonService;

    @Autowired
    KafkaEventProducer producer;

    @Autowired
    DigitalKontaktinfoConsumer kontaktinfoConsumer;

    public DigitalKontaktinformasjonTo.DigitalKontaktinfo getKontaktInfoByFnr(Doknotifikasjon doknotifikasjon) {

        DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinfo = kontaktinfoConsumer.hentDigitalKontaktinfo(
                doknotifikasjon.getFodselsnummer()
        );

        if (kontaktinfo == null) {
            publishDoknotikfikasjonStatusDKIF(doknotifikasjon, FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET);
        } else if(kontaktinfo.isReservert()) {
            publishDoknotikfikasjonStatusDKIF(doknotifikasjon, FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT);
        } else if (kontaktinfo.isKanVarsles()) {
            publishDoknotikfikasjonStatusDKIF(doknotifikasjon, FEILET_USER_DP_NOT_HAVE_VALID_CONTACT_INFORMATION);
        } else if(kontaktinfo.getEpostadresse() != null && kontaktinfo.getMobiltelefonnummer() != null) {
            publishDoknotikfikasjonStatusDKIF(doknotifikasjon, FEILET_USER_DP_NOT_HAVE_VALID_CONTACT_INFORMATION);
        }

        return kontaktinfo;
    }

    public void publishDoknotikfikasjonStatusDKIF(Doknotifikasjon doknotifikasjon, String message) {
        StatusProducer.publishDoknotikfikasjonStatusFeilet(
                doknotifikasjon.getBestillingsId(),
                doknotifikasjon.getBestillerId(),
                message,
                null
        );

        //TODO throw new exception
    }


    public void  createNotifikasjonFromDoknotifikasjon(Doknotifikasjon doknotifikasjon, DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinformasjon) {
        if (notifikasjonService.notifikasjonExistByBestillingsId(doknotifikasjon.getBestillingsId())) {
            StatusProducer.publishDoknotikfikasjonStatusFeilet(
                    doknotifikasjon.getBestillingsId(),
                    doknotifikasjon.getBestillerId(),
                    FEILET_ALREADY_EXIST_IN_DATABASE,
                    null
            );
            //TODO throw new exception
        }

        Notifikasjon notifikasjon = this.createNotifikasjonByDoknotikasjon(doknotifikasjon);

        if (kontaktinformasjon.getEpostadresse() != null) {
            this.createNotifikasjonDistrubisjon(doknotifikasjon.getEpostTekst(),Kanal.EPOST, notifikasjon, kontaktinformasjon.getEpostadresse(), doknotifikasjon.getTittel());
        }
        if (kontaktinformasjon.getMobiltelefonnummer() != null) {
            this.createNotifikasjonDistrubisjon(doknotifikasjon.getSmsTekst(),Kanal.SMS, notifikasjon, kontaktinformasjon.getMobiltelefonnummer(), doknotifikasjon.getTittel());
        }
    }

    public Notifikasjon createNotifikasjonByDoknotikasjon(Doknotifikasjon doknotifikasjon) {
        LocalDate nesteRenotifikasjonDato = null;

        if (doknotifikasjon.getAntallRenotifikasjoner() != null && doknotifikasjon.getAntallRenotifikasjoner() > 1) {
            nesteRenotifikasjonDato = LocalDate.now().plusDays(doknotifikasjon.getAntallRenotifikasjoner());
        }

        Notifikasjon notifikasjon = Notifikasjon.builder()
                .bestillingId(doknotifikasjon.getBestillingsId())
                .bestillerId(doknotifikasjon.getBestillerId())
                .mottakerId(doknotifikasjon.getFodselsnummer())
                .mottakerIdType(MottakerIdType.FNR)
                .status(Status.OPPRETTET)
                .antallRenotifikasjoner(doknotifikasjon.getAntallRenotifikasjoner())
                .renotifikasjonIntervall(doknotifikasjon.getRenotifikasjonIntervall())
                .nesteRenotifikasjonDato(nesteRenotifikasjonDato)
                .prefererteKanaler(doknotifikasjon.getPrefererteKanaler())
                .opprettetAv(doknotifikasjon.getBestillerId())
                .build();

        return notifikasjonService.save(notifikasjon);
    }

    public NotifikasjonDistribusjon createNotifikasjonDistrubisjon(String tekst, Kanal kanal, Notifikasjon notifikasjon, String kontaktinformasjon, String tittel) {
        NotifikasjonDistribusjon notifikasjonDistribusjon = NotifikasjonDistribusjon.builder()
                .notifikasjonId(notifikasjon)
                .status(Status.OPPRETTET)
                .kanal(kanal)
                .kontaktInfo(kontaktinformasjon)
                .tittel(tittel)
                .tekst(tekst)
                .build();

        return notifikasjonDistrbusjonService.save(notifikasjonDistribusjon);
    }


    public void validateAvroDoknotifikasjon(Doknotifikasjon doknotifikasjon)  throws InvalidAvroSchemaFieldException
    {
        this.validateString(doknotifikasjon,doknotifikasjon.getBestillingsId());
        this.validateString(doknotifikasjon,doknotifikasjon.getBestillerId());
        this.validateString(doknotifikasjon,doknotifikasjon.getFodselsnummer());
        this.validateString(doknotifikasjon,doknotifikasjon.getTittel());
        this.validateString(doknotifikasjon,doknotifikasjon.getEpostTekst());
        this.validateString(doknotifikasjon,doknotifikasjon.getSmsTekst());
        this.validateString(doknotifikasjon,doknotifikasjon.getPrefererteKanaler());
    }

    public void validateString(Doknotifikasjon doknotifikasjon, String string) throws InvalidAvroSchemaFieldException {
        if (string.trim().isEmpty() || string.equals("null")) {
            StatusProducer.publishDoknotikfikasjonStatusFeilet(
                    doknotifikasjon.getBestillingsId(),
                    Doknotifikasjon.newBuilder().getBestillerId(),
                    "påkrevd felt <feltnavn> ikke satt", // todo create message for message,
                    null
            );
            throw new InvalidAvroSchemaFieldException("");         //TODO throw new exception
        }
    }

    public void publishDoknotikfikasjonSms(String bestillingsId) {
        DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(bestillingsId);
        producer.publish(
                KAFKA_TOPIC_DOK_NOTIFKASJON_SMS,
                doknotifikasjonSms,
                System.currentTimeMillis()
        );
    }

    public void publishDoknotikfikasjonEpost(String bestillingsId) {
        DoknotifikasjonEpost doknotifikasjonEpost = new DoknotifikasjonEpost(bestillingsId);
        producer.publish(
                KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST,
                doknotifikasjonEpost,
                System.currentTimeMillis()
        );
    }
}
