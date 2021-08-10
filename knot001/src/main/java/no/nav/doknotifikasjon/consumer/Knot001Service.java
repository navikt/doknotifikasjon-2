package no.nav.doknotifikasjon.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.consumer.sikkerhetsnivaa.AuthLevelResponse;
import no.nav.doknotifikasjon.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.doknotifikasjon.exception.functional.DigitalKontaktinformasjonFunctionalException;
import no.nav.doknotifikasjon.exception.functional.DuplicateNotifikasjonInDBException;
import no.nav.doknotifikasjon.exception.functional.KontaktInfoValidationFunctionalException;
import no.nav.doknotifikasjon.exception.functional.SikkerhetsnivaaFunctionalException;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.MottakerIdType;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_FUNCTIONAL_EXCEPTION_DKIF;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_FUNCTIONAL_EXCEPTION_SIKKERHETSNIVAA;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SIKKERHETSNIVAA;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_TECHNICAL_EXCEPTION_DATABASE;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.INFO_ALREADY_EXIST_IN_DATABASE;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.OVERSENDT_NOTIFIKASJON_PROCESSED;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS;


@Slf4j
@Component
public class Knot001Service {

	private final KafkaStatusEventProducer statusProducer;
	private final NotifikasjonService notifkasjonService;
	private final KafkaEventProducer producer;
	private final DigitalKontaktinfoConsumer kontaktinfoConsumer;
	private final SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer;

	@Inject
	Knot001Service(
			DigitalKontaktinfoConsumer kontaktinfoConsumer,
			KafkaEventProducer producer,
			NotifikasjonService notifkasjonService,
			KafkaStatusEventProducer statusProducer,
			SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer
	) {
		this.statusProducer = statusProducer;
		this.notifkasjonService = notifkasjonService;
		this.producer = producer;
		this.kontaktinfoConsumer = kontaktinfoConsumer;
		this.sikkerhetsnivaaConsumer = sikkerhetsnivaaConsumer;
	}

	public void processDoknotifikasjon(DoknotifikasjonTO doknotifikasjon) {
		log.info("Knot001 begynner med prossesering av kafka event med bestillingsId={}", doknotifikasjon.getBestillingsId());

		this.checkSikkerhetsnivaa(doknotifikasjon);

		DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinfo = this.getKontaktInfoByFnr(doknotifikasjon);
		Notifikasjon notifikasjon = this.createNotifikasjonByDoknotifikasjonTO(doknotifikasjon, kontaktinfo);
		notifikasjon.getNotifikasjonDistribusjon().forEach(n -> this.publishDoknotifikasjonDistrubisjon(n.getId(), n.getKanal()));

		statusProducer.publishDoknotikfikasjonStatusOversendt(
				doknotifikasjon.getBestillingsId(),
				doknotifikasjon.getBestillerId(),
				OVERSENDT_NOTIFIKASJON_PROCESSED,
				null
		);
		log.info("Sender en DoknotifikasjonStatus med status={} til topic={} for bestillingsId={}", Status.OVERSENDT, KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS, doknotifikasjon.getBestillingsId());
	}

	public DigitalKontaktinformasjonTo.DigitalKontaktinfo getKontaktInfoByFnr(DoknotifikasjonTO doknotifikasjon) {
		String fnrTrimmed = doknotifikasjon.getFodselsnummer().trim();
		DigitalKontaktinformasjonTo digitalKontaktinformasjon;

		try {
			log.info("Henter kontaktinfo fra DKIF for bestilling med bestillingsId={}", doknotifikasjon.getBestillingsId());
			digitalKontaktinformasjon = kontaktinfoConsumer.hentDigitalKontaktinfo(fnrTrimmed);
		} catch (DigitalKontaktinformasjonFunctionalException e) {
			statusProducer.publishDoknotikfikasjonStatusFeilet(
					doknotifikasjon.getBestillingsId(),
					doknotifikasjon.getBestillerId(),
					FEILET_FUNCTIONAL_EXCEPTION_DKIF,
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
			publishDoknotikfikasjonStatusIfValidationOfKontaktinfoFails(doknotifikasjon, FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION);
		} else if ((kontaktinfo.getEpostadresse() == null || kontaktinfo.getEpostadresse().trim().isEmpty()) &&
				(kontaktinfo.getMobiltelefonnummer() == null || kontaktinfo.getMobiltelefonnummer().trim().isEmpty())) {
			publishDoknotikfikasjonStatusIfValidationOfKontaktinfoFails(doknotifikasjon, FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION);
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

	public void checkSikkerhetsnivaa(DoknotifikasjonTO doknotifikasjonTO) {
		if (doknotifikasjonTO.getSikkerhetsnivaa() == 4) {
			AuthLevelResponse authLevelResponse;
			try {
				log.info("Knot001 gjør oppslag mot sikkerhetsnivaa for hendelse med bestillingsId={}", doknotifikasjonTO.getBestillingsId());
				authLevelResponse = sikkerhetsnivaaConsumer.lookupAuthLevel(doknotifikasjonTO.getFodselsnummer());
			} catch (SikkerhetsnivaaFunctionalException exception) {
				statusProducer.publishDoknotikfikasjonStatusFeilet(
						doknotifikasjonTO.getBestillingsId(),
						doknotifikasjonTO.getBestillerId(),
						FEILET_FUNCTIONAL_EXCEPTION_SIKKERHETSNIVAA,
						null
				);
				log.warn("Problemer med å hente sikkerhetsnivaa for bestillingsId={}. Feilmelding: {}", doknotifikasjonTO.getBestillingsId(), exception.getMessage());
				throw exception;
			}
			if (!authLevelResponse.isHarbruktnivaa4()) {
				statusProducer.publishDoknotikfikasjonStatusFeilet(
						doknotifikasjonTO.getBestillingsId(),
						doknotifikasjonTO.getBestillerId(),
						FEILET_SIKKERHETSNIVAA,
						null);
				throw new SikkerhetsnivaaFunctionalException(FEILET_SIKKERHETSNIVAA);
			}
		}
	}

	public Notifikasjon createNotifikasjonByDoknotifikasjonTO(DoknotifikasjonTO doknotifikasjon, DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinformasjon) {
		log.info("Knot001 starter med opprettelse av notifikasjon til databasen for bestillingsId={}.", doknotifikasjon.getBestillingsId());

		boolean shouldStoreSms = doknotifikasjon.getPrefererteKanaler().contains(Kanal.SMS);
		boolean shouldStoreEpost = doknotifikasjon.getPrefererteKanaler().contains(Kanal.EPOST);

		if (notifkasjonService.existsByBestillingsId("doknotifikasjon.getBestillingsId()")) {
			statusProducer.publishDoknotikfikasjonStatusInfo(
					doknotifikasjon.getBestillingsId(),
					doknotifikasjon.getBestillerId(),
					INFO_ALREADY_EXIST_IN_DATABASE,
					null
			);
			throw new DuplicateNotifikasjonInDBException(String.format("Notifikasjon med bestillingsId=%s finnes allerede i notifikasjonsdatabasen. Avslutter behandlingen.",
					doknotifikasjon.getBestillingsId()));
		}

		Notifikasjon notifikasjon = this.createNotifikasjonByDoknotifikasjonTO(doknotifikasjon);

		if (kontaktinformasjon.getEpostadresse() != null && (shouldStoreEpost || kontaktinformasjon.getMobiltelefonnummer() == null)) {
			this.createNotifikasjonDistrubisjon(doknotifikasjon.getEpostTekst(), Kanal.EPOST, notifikasjon, kontaktinformasjon.getEpostadresse(), doknotifikasjon.getTittel());
			log.info("Knot001 har opprettet notifikasjonDistribusjon med kanal EPOST for bestilling med bestillingsId={}", doknotifikasjon.getBestillingsId());
		}
		if (kontaktinformasjon.getMobiltelefonnummer() != null && (shouldStoreSms || kontaktinformasjon.getEpostadresse() == null)) {
			this.createNotifikasjonDistrubisjon(doknotifikasjon.getSmsTekst(), Kanal.SMS, notifikasjon, kontaktinformasjon.getMobiltelefonnummer(), doknotifikasjon.getTittel());
			log.info("Knot001 har opprettet notifikasjonDistribusjon med kanal SMS for bestilling med bestillingsId={}", doknotifikasjon.getBestillingsId());
		}

		try {
			return notifkasjonService.save(notifikasjon);
		} catch (DataIntegrityViolationException e) {
			statusProducer.publishDoknotikfikasjonStatusFeilet(
					doknotifikasjon.getBestillingsId(),
					doknotifikasjon.getBestillerId(),
					FEILET_TECHNICAL_EXCEPTION_DATABASE,
					null
			);
			throw e;
		}
	}


	public Notifikasjon createNotifikasjonByDoknotifikasjonTO(DoknotifikasjonTO doknotifikasjonTO) {
		LocalDate nesteRenotifikasjonDato = null;

		if (doknotifikasjonTO.getAntallRenotifikasjoner() != null && doknotifikasjonTO.getAntallRenotifikasjoner() > 0 && doknotifikasjonTO.getRenotifikasjonIntervall() != null) {
			nesteRenotifikasjonDato = LocalDate.now().plusDays(doknotifikasjonTO.getRenotifikasjonIntervall());
		}

		return Notifikasjon.builder()
				.bestillingsId(doknotifikasjonTO.getBestillingsId())
				.bestillerId(doknotifikasjonTO.getBestillerId())
				.mottakerId(doknotifikasjonTO.getFodselsnummer())
				.mottakerIdType(MottakerIdType.FNR)
				.status(Status.OPPRETTET)
				.antallRenotifikasjoner(doknotifikasjonTO.getAntallRenotifikasjoner())
				.renotifikasjonIntervall(doknotifikasjonTO.getRenotifikasjonIntervall())
				.nesteRenotifikasjonDato(nesteRenotifikasjonDato)
				.prefererteKanaler(this.buildPrefererteKanaler(doknotifikasjonTO.getPrefererteKanaler()))
				.opprettetAv(doknotifikasjonTO.getBestillerId())
				.opprettetDato(LocalDateTime.now())
				.notifikasjonDistribusjon(new HashSet<>())
				.build();
	}

	private String buildPrefererteKanaler(List<Kanal> prefererteKanaler) {
		StringBuilder stringBuilder = new StringBuilder();
		prefererteKanaler.forEach(kanal -> stringBuilder.append(prefererteKanaler.indexOf(kanal) == prefererteKanaler.size() - 1 ? kanal.toString() : kanal.toString() + ", "));
		return stringBuilder.toString();
	}

	public void createNotifikasjonDistrubisjon(String tekst, Kanal kanal, Notifikasjon notifikasjon, String kontaktinformasjon, String tittel) {
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
			notifikasjon.getNotifikasjonDistribusjon().add(notifikasjonDistribusjon);
	}

	public void publishDoknotifikasjonDistrubisjon(Integer bestillingsId, Kanal kanal) {
		String topic = Kanal.EPOST.equals(kanal) ? KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST : KAFKA_TOPIC_DOK_NOTIFKASJON_SMS;

		log.info("Publiserer bestilling til kafka topic {}, med bestillingsId={}", topic, bestillingsId);
		DoknotifikasjonEpost doknotifikasjonEpostTo = new DoknotifikasjonEpost(bestillingsId);
		producer.publish(
				topic,
				doknotifikasjonEpostTo
		);
	}
}