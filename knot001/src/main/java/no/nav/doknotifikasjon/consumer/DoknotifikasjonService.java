package no.nav.doknotifikasjon.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.KafkaProducer.KafkaDoknotifikasjonStatusProducer;
import no.nav.doknotifikasjon.KafkaProducer.KafkaEventProducer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.exception.functional.DigitalKontaktinformasjonFunctionalException;
import no.nav.doknotifikasjon.exception.functional.InvalidAvroSchemaFieldException;
import no.nav.doknotifikasjon.exception.technical.DigitalKontaktinformasjonTechnicalException;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDate;

import static java.lang.String.format;
import static no.nav.doknotifikasjon.KafkaProducer.DoknotifikasjonStatusMessage.FEILET_ALREADY_EXIST_IN_DATABASE;
import static no.nav.doknotifikasjon.KafkaProducer.DoknotifikasjonStatusMessage.FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER;
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
        DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinfo;

        try {
            kontaktinfo = kontaktinfoConsumer.hentDigitalKontaktinfo(doknotifikasjon.getFodselsnummer());
        } catch (Exception e) {
            publishDoknotikfikasjonStatusDKIF(doknotifikasjon, "Melding"); // TODO change name of message
            throw e;
        }

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

        throw new InvalidAvroSchemaFieldException("DKIF exception BestillingsId: [" + doknotifikasjon.getBestillingsId() + "], BestillerId[" + doknotifikasjon.getBestillerId() + "]"); //TODO throw new exception
    }


    public void  createNotifikasjonFromDoknotifikasjon(Doknotifikasjon doknotifikasjon, DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinformasjon) {
        if (notifikasjonService.notifikasjonExistByBestillingsId(doknotifikasjon.getBestillingsId())) {
            StatusProducer.publishDoknotikfikasjonStatusFeilet(
                    doknotifikasjon.getBestillingsId(),
                    doknotifikasjon.getBestillerId(),
                    FEILET_ALREADY_EXIST_IN_DATABASE,
                    null
            );
            throw new InvalidAvroSchemaFieldException("BestillingsId already exist in database. BestillingsId: [" + doknotifikasjon.getBestillingsId() + "], BestillerId[" + doknotifikasjon.getBestillerId() + "]"); //TODO throw new exception
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
        this.validateString(doknotifikasjon, doknotifikasjon.getBestillingsId(), "BestillingsId");
        this.validateString(doknotifikasjon, doknotifikasjon.getBestillerId(),"BestillerId");
        this.validateString(doknotifikasjon, doknotifikasjon.getFodselsnummer(), "Fodselsnummer");
        this.validateString(doknotifikasjon, doknotifikasjon.getTittel(), "Tittel");
        this.validateString(doknotifikasjon, doknotifikasjon.getEpostTekst(), "EpostTekst");
        this.validateString(doknotifikasjon, doknotifikasjon.getSmsTekst(), "SmsTekst");
        this.validateString(doknotifikasjon, doknotifikasjon.getPrefererteKanaler(), "PrefererteKanaler");
        this.validateNumber(doknotifikasjon, doknotifikasjon.getAntallRenotifikasjoner(), "AntallRenotifikasjoner");
        this.validateNumber(doknotifikasjon, doknotifikasjon.getRenotifikasjonIntervall(), "RenotifikasjonIntervall");

        if ((doknotifikasjon.getAntallRenotifikasjoner() != null || doknotifikasjon.getAntallRenotifikasjoner() == 0) && doknotifikasjon.getRenotifikasjonIntervall() < 1) {
            StatusProducer.publishDoknotikfikasjonStatusFeilet(
                    doknotifikasjon.getBestillingsId(),
                    Doknotifikasjon.newBuilder().getBestillerId(),
                    FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER,
                    null
            );
        }
    }

    public void validateString(Doknotifikasjon doknotifikasjon, String string, String fieldName) throws InvalidAvroSchemaFieldException {
        if (string.trim().isEmpty() || string.equals("null")) {
            StatusProducer.publishDoknotikfikasjonStatusFeilet(
                    doknotifikasjon.getBestillingsId(),
                    Doknotifikasjon.newBuilder().getBestillerId(),
                    "påkrevd felt " + fieldName + " ikke satt", // todo create message
                    null
            );
            throw new InvalidAvroSchemaFieldException("Field [" + fieldName + "] was not valid. BestillingsId: [" + doknotifikasjon.getBestillingsId() + "], BestillerId[" + doknotifikasjon.getBestillerId() + "]");
        }
    }

    public void validateNumber(Doknotifikasjon doknotifikasjon, Integer number, String fieldName) throws InvalidAvroSchemaFieldException {
        if (number > 0) {
            StatusProducer.publishDoknotikfikasjonStatusFeilet(
                    doknotifikasjon.getBestillingsId(),
                    Doknotifikasjon.newBuilder().getBestillerId(),
                    "påkrevd felt " + fieldName + " kan ikke være negativ", // todo create message
                    null
            );
            throw new InvalidAvroSchemaFieldException("Field [" + fieldName + "] was not valid. BestillingsId: [" + doknotifikasjon.getBestillingsId() + "], BestillerId[" + doknotifikasjon.getBestillerId() + "]");        }
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
