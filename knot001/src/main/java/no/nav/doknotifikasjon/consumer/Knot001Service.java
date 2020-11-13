package no.nav.doknotifikasjon.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.exception.functional.DigitalKontaktinformasjonFunctionalException;
import no.nav.doknotifikasjon.exception.functional.DuplicateNotifikasjonInDBException;
import no.nav.doknotifikasjon.exception.functional.KontaktInfoValidationFunctionalException;
import no.nav.doknotifikasjon.exception.technical.DigitalKontaktinformasjonTechnicalException;
import no.nav.doknotifikasjon.exception.technical.StsTechnicalException;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.MottakerIdType;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static no.nav.doknotifikasjon.constants.RetryConstants.DELAY_LONG;
import static no.nav.doknotifikasjon.constants.RetryConstants.MAX_INT;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_DP_NOT_HAVE_VALID_CONTACT_INFORMATION;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.INFO_ALREADY_EXIST_IN_DATABASE;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.INFO_CANT_CONNECT_TO_DKIF;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.OVERSENDT_NOTIFIKASJON_PROCESSED;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS;


@Slf4j
@Component
public class Knot001Service {

	private final KafkaStatusEventProducer statusProducer;
	private final NotifikasjonRepository notifikasjonRepository;
	private final KafkaEventProducer producer;
	private final DigitalKontaktinfoConsumer kontaktinfoConsumer;
	private final NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;

	Knot001Service(
			DigitalKontaktinfoConsumer kontaktinfoConsumer,
			KafkaEventProducer producer,
			NotifikasjonRepository notifikasjonRepository,
			KafkaStatusEventProducer statusProducer,
			NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository
	) {
		this.statusProducer = statusProducer;
		this.notifikasjonRepository = notifikasjonRepository;
		this.producer = producer;
		this.kontaktinfoConsumer = kontaktinfoConsumer;
		this.notifikasjonDistribusjonRepository = notifikasjonDistribusjonRepository;
	}

	public void processDoknotifikasjon(DoknotifikasjonTO doknotifikasjon) {
		log.info("Begynner med prossesering av kafka event med bestillingsId={}", doknotifikasjon.getBestillingsId());

		DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinfo = this.getKontaktInfoByFnr(doknotifikasjon);
		this.createNotifikasjonByDoknotifikasjonAndNotifikasjonDistrubisjon(doknotifikasjon, kontaktinfo);

		statusProducer.publishDoknotikfikasjonStatusOversendt(
				doknotifikasjon.getBestillingsId(),
				doknotifikasjon.getBestillerId(),
				OVERSENDT_NOTIFIKASJON_PROCESSED,
				null
		);
		log.info("Sender en DoknotifikasjonStatus med status {} til topic {} for bestillingsId {}", Status.OVERSENDT, KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS, doknotifikasjon.getBestillingsId());
	}

	@Retryable(
			include = {DigitalKontaktinformasjonTechnicalException.class, DigitalKontaktinformasjonFunctionalException.class, StsTechnicalException.class},
			maxAttempts = MAX_INT,
			backoff = @Backoff(delay = DELAY_LONG)
	)
	public DigitalKontaktinformasjonTo.DigitalKontaktinfo getKontaktInfoByFnr(DoknotifikasjonTO doknotifikasjon) {
		String fnrTrimmed = doknotifikasjon.getFodselsnummer().trim();
		DigitalKontaktinformasjonTo digitalKontaktinformasjon;

		try {
			log.info("Henter kontaktinfo fra DKIF for bestilling med bestillingsId={}", doknotifikasjon.getBestillingsId());
			digitalKontaktinformasjon = kontaktinfoConsumer.hentDigitalKontaktinfo(fnrTrimmed);
		} catch (DigitalKontaktinformasjonTechnicalException | DigitalKontaktinformasjonFunctionalException | StsTechnicalException e) {
			statusProducer.publishDoknotikfikasjonStatusInfo(
					doknotifikasjon.getBestillingsId(),
					doknotifikasjon.getBestillerId(),
					INFO_CANT_CONNECT_TO_DKIF,
					null
			);
			log.warn("Problemer med å hente kontaktinfo med bestillingsId={}. Feilmelding: {}", doknotifikasjon.getBestillingsId(), e.getMessage());
			throw e;
		}

		DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinfo = digitalKontaktinformasjon.getKontaktinfo() != null ? digitalKontaktinformasjon.getKontaktinfo().get(fnrTrimmed) : null;

		if (kontaktinfo == null) {
			if (digitalKontaktinformasjon.getFeil() != null && digitalKontaktinformasjon.getFeil().get(fnrTrimmed) != null && digitalKontaktinformasjon.getFeil().get(fnrTrimmed).getMelding() != null) {
				publishDoknotikfikasjonStatusIfValidationOfKontaktinfoFails(doknotifikasjon, digitalKontaktinformasjon.getFeil().get(fnrTrimmed).getMelding());
			}
			publishDoknotikfikasjonStatusIfValidationOfKontaktinfoFails(doknotifikasjon, FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET);
		} else if (kontaktinfo.isReservert()) {
			publishDoknotikfikasjonStatusIfValidationOfKontaktinfoFails(doknotifikasjon, FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT);
		} else if (!kontaktinfo.isKanVarsles()) {
			publishDoknotikfikasjonStatusIfValidationOfKontaktinfoFails(doknotifikasjon, FEILET_USER_DP_NOT_HAVE_VALID_CONTACT_INFORMATION);
		} else if ((kontaktinfo.getEpostadresse() == null || kontaktinfo.getEpostadresse().trim().isEmpty()) &&
				(kontaktinfo.getMobiltelefonnummer() == null || kontaktinfo.getMobiltelefonnummer().trim().isEmpty())) {
			publishDoknotikfikasjonStatusIfValidationOfKontaktinfoFails(doknotifikasjon, FEILET_USER_DP_NOT_HAVE_VALID_CONTACT_INFORMATION);
		}

		return kontaktinfo;
	}

	public void publishDoknotikfikasjonStatusIfValidationOfKontaktinfoFails(DoknotifikasjonTO doknotifikasjon, String message) {
		statusProducer.publishDoknotikfikasjonStatusFeilet(
				doknotifikasjon.getBestillingsId(),
				doknotifikasjon.getBestillerId(),
				message,
				null
		);
		throw new KontaktInfoValidationFunctionalException(String.format("Problemer med å hente kontaktinfo fra DKIF med bestillingsId=%s. Feilmelding: %s", doknotifikasjon.getBestillingsId(), message));
	}

	@Retryable(
			exclude = DuplicateNotifikasjonInDBException.class,
			maxAttempts = MAX_INT,
			backoff = @Backoff(delay = DELAY_LONG)
	)
	public void createNotifikasjonByDoknotifikasjonAndNotifikasjonDistrubisjon(DoknotifikasjonTO doknotifikasjon, DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinformasjon) {
		log.info("Lagrer bestillingen til databasen med bestillingsId={}", doknotifikasjon.getBestillingsId());
		boolean shouldStoreSms = doknotifikasjon.getPrefererteKanaler().contains(Kanal.SMS);
		boolean shouldStoreEpost = doknotifikasjon.getPrefererteKanaler().contains(Kanal.EPOST);

		if (notifikasjonRepository.existsByBestillingsId(doknotifikasjon.getBestillingsId())) {
			statusProducer.publishDoknotikfikasjonStatusInfo(
					doknotifikasjon.getBestillingsId(),
					doknotifikasjon.getBestillerId(),
					INFO_ALREADY_EXIST_IN_DATABASE,
					null
			);
			throw new DuplicateNotifikasjonInDBException(String.format("Notifikasjon med bestillingsId=%s finnes allerede i notifikasjonsdatabasen. Avslutter behandlingen.",
					doknotifikasjon.getBestillingsId()));
		}

		Notifikasjon notifikasjon = this.createNotifikasjonByDoknotifikasjonAndNotifikasjonDistrubisjon(doknotifikasjon);

		if (kontaktinformasjon.getEpostadresse() != null && (shouldStoreEpost || kontaktinformasjon.getMobiltelefonnummer() == null)) {
			NotifikasjonDistribusjon email = this.createNotifikasjonDistrubisjon(doknotifikasjon.getEpostTekst(), Kanal.EPOST, notifikasjon, kontaktinformasjon.getEpostadresse(), doknotifikasjon.getTittel());
			this.publishDoknotifikasjonEpost(email.getId());
		}
		if (kontaktinformasjon.getMobiltelefonnummer() != null && (shouldStoreSms || kontaktinformasjon.getEpostadresse() == null)) {
			NotifikasjonDistribusjon sms = this.createNotifikasjonDistrubisjon(doknotifikasjon.getSmsTekst(), Kanal.SMS, notifikasjon, kontaktinformasjon.getMobiltelefonnummer(), doknotifikasjon.getTittel());
			this.publishDoknotifikasjonSms(sms.getId());
		}
	}

	public Notifikasjon createNotifikasjonByDoknotifikasjonAndNotifikasjonDistrubisjon(DoknotifikasjonTO doknotifikasjon) {
		LocalDate nesteRenotifikasjonDato = null;

		if (doknotifikasjon.getAntallRenotifikasjoner() != null && doknotifikasjon.getAntallRenotifikasjoner() > 1) {
			nesteRenotifikasjonDato = LocalDate.now().plusDays(doknotifikasjon.getAntallRenotifikasjoner());
		}

		Notifikasjon notifikasjon = Notifikasjon.builder()
				.bestillingsId(doknotifikasjon.getBestillingsId())
				.bestillerId(doknotifikasjon.getBestillerId())
				.mottakerId(doknotifikasjon.getFodselsnummer())
				.mottakerIdType(MottakerIdType.FNR)
				.status(Status.OPPRETTET)
				.antallRenotifikasjoner(doknotifikasjon.getAntallRenotifikasjoner())
				.renotifikasjonIntervall(doknotifikasjon.getRenotifikasjonIntervall())
				.nesteRenotifikasjonDato(nesteRenotifikasjonDato)
				.prefererteKanaler(this.buildPrefererteKanaler(doknotifikasjon.getPrefererteKanaler()))
				.opprettetAv(doknotifikasjon.getBestillerId())
				.opprettetDato(LocalDateTime.now())
				.notifikasjonDistribusjon(new HashSet<>())
				.build();

		return notifikasjonRepository.save(notifikasjon);
	}

	private String buildPrefererteKanaler(List<Kanal> prefererteKanaler) {
		StringBuilder stringBuilder = new StringBuilder();
		prefererteKanaler.forEach(s -> stringBuilder.append(prefererteKanaler.indexOf(s) == prefererteKanaler.size() - 1 ? s.toString() : s.toString() + ", "));
		return stringBuilder.toString();
	}

	public NotifikasjonDistribusjon createNotifikasjonDistrubisjon(String tekst, Kanal kanal, Notifikasjon notifikasjon, String kontaktinformasjon, String tittel) {
		NotifikasjonDistribusjon notifikasjonDistribusjon = NotifikasjonDistribusjon.builder()
				.notifikasjon(notifikasjon)
				.status(Status.OPPRETTET)
				.kanal(kanal)
				.kontaktInfo(kontaktinformasjon)
				.tittel(tittel)
				.tekst(tekst)
				.opprettetDato(LocalDateTime.now())
				.opprettetAv(notifikasjon.getBestillingsId())
				.build();

		return notifikasjonDistribusjonRepository.save(notifikasjonDistribusjon);
	}

	public void publishDoknotifikasjonSms(Integer bestillingsId) {
		log.info("Publiserer bestilling til kafka topic {}, med bestillingsId={}", KAFKA_TOPIC_DOK_NOTIFKASJON_SMS, bestillingsId);
		DoknotifikasjonSms doknotifikasjonSms = new DoknotifikasjonSms(bestillingsId);
		producer.publish(
				KAFKA_TOPIC_DOK_NOTIFKASJON_SMS,
				doknotifikasjonSms
		);
	}

	public void publishDoknotifikasjonEpost(Integer bestillingsId) {
		log.info("Publiserer bestilling til kafka topic {}, med bestillingsId={}", KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST, bestillingsId);
		DoknotifikasjonEpost doknotifikasjonEpostTo = new DoknotifikasjonEpost(bestillingsId);
		producer.publish(
				KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST,
				doknotifikasjonEpostTo
		);
	}
}
