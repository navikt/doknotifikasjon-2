package no.nav.doknotifikasjon.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.DuplicateNotifikasjonInDBException;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.kodeverk.Kanal;
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

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_TECHNICAL_EXCEPTION_DATABASE;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.INFO_ALREADY_EXIST_IN_DATABASE;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.OVERSENDT_NOTIFIKASJON_PROCESSED;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_SMS;
import static no.nav.doknotifikasjon.kafka.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS;
import static no.nav.doknotifikasjon.kodeverk.Kanal.*;
import static no.nav.doknotifikasjon.kodeverk.MottakerIdType.FNR;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;
import static no.nav.doknotifikasjon.kodeverk.Status.OVERSENDT;


@Slf4j
@Component
public class Knot006Service {

	private final KafkaStatusEventProducer statusProducer;
	private final NotifikasjonService notifkasjonService;
	private final KafkaEventProducer producer;

	@Inject
	Knot006Service(
			KafkaEventProducer producer,
			NotifikasjonService notifkasjonService,
			KafkaStatusEventProducer statusProducer
	) {
		this.statusProducer = statusProducer;
		this.notifkasjonService = notifkasjonService;
		this.producer = producer;
	}

	public void processNotifikasjonMedkontaktInfo(NotifikasjonMedKontaktInfoTO notifikasjonMedKontaktInfoTO) {
		log.info("Knot006 begynner med prossesering av kafka event med bestillingsId={}", notifikasjonMedKontaktInfoTO.getBestillingsId());

		Notifikasjon notifikasjon = this.createNotifikasjonByNotifikasjonMedKontaktInfoTO(notifikasjonMedKontaktInfoTO);
		notifikasjon.getNotifikasjonDistribusjon().forEach(n -> this.publishNotifikasjonDistrubisjon(n.getId(), n.getKanal()));

		statusProducer.publishDoknotikfikasjonStatusOversendt(
				notifikasjonMedKontaktInfoTO.getBestillingsId(),
				notifikasjonMedKontaktInfoTO.getBestillerId(),
				OVERSENDT_NOTIFIKASJON_PROCESSED,
				null
		);

		log.info("Sender notifikasjon med status={} til topic={} med bestillingsId={}", OVERSENDT, KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS, notifikasjonMedKontaktInfoTO.getBestillingsId());
	}

	public Notifikasjon createNotifikasjonByNotifikasjonMedKontaktInfoTO(NotifikasjonMedKontaktInfoTO doknotifikasjon) {
		log.info("Knot006 starter med opprettelse av notifikasjon til databasen for bestillingsId={}.", doknotifikasjon.getBestillingsId());

		boolean shouldStoreSms = doknotifikasjon.getPrefererteKanaler().contains(SMS);
		boolean shouldStoreEpost = doknotifikasjon.getPrefererteKanaler().contains(EPOST);

		if (notifkasjonService.existsByBestillingsId(doknotifikasjon.getBestillingsId())) {
			statusProducer.publishDoknotikfikasjonStatusInfo(
					doknotifikasjon.getBestillingsId(),
					doknotifikasjon.getBestillerId(),
					INFO_ALREADY_EXIST_IN_DATABASE,
					null
			);
			throw new DuplicateNotifikasjonInDBException(String.format("Notifikasjon med bestillingsId=%s finnes allerede i notifikasjonsdatabasen. Avslutter behandlingen.",
					doknotifikasjon.getBestillingsId()));
		}

		Notifikasjon notifikasjon = this.buildNotifikasjonByDoknotifikasjonTO(doknotifikasjon);

		if (doknotifikasjon.getEpostadresse() != null && (shouldStoreEpost || doknotifikasjon.getMobiltelefonnummer() == null)) {
			this.createNotifikasjonDistrubisjon(doknotifikasjon.getEpostTekst(), EPOST, notifikasjon, doknotifikasjon.getEpostadresse(), doknotifikasjon.getTittel());
			log.info("Knot006 har opprettet notifikasjonDistribusjon med kanal EPOST for bestilling med bestillingsId={}", doknotifikasjon.getBestillingsId());
		}
		if (doknotifikasjon.getMobiltelefonnummer() != null && (shouldStoreSms || doknotifikasjon.getEpostadresse() == null)) {
			this.createNotifikasjonDistrubisjon(doknotifikasjon.getSmsTekst(), SMS, notifikasjon, doknotifikasjon.getMobiltelefonnummer(), doknotifikasjon.getTittel());
			log.info("Knot006 har opprettet notifikasjonDistribusjon med kanal SMS for bestilling med bestillingsId={}", doknotifikasjon.getBestillingsId());
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


	public Notifikasjon buildNotifikasjonByDoknotifikasjonTO(NotifikasjonMedKontaktInfoTO notifikasjonMedKontaktInfoTO) {
		LocalDate nesteRenotifikasjonDato = null;

		if (notifikasjonMedKontaktInfoTO.getAntallRenotifikasjoner() != null && notifikasjonMedKontaktInfoTO.getAntallRenotifikasjoner() > 0 && notifikasjonMedKontaktInfoTO.getRenotifikasjonIntervall() != null) {
			nesteRenotifikasjonDato = LocalDate.now().plusDays(notifikasjonMedKontaktInfoTO.getRenotifikasjonIntervall());
		}

		return Notifikasjon.builder()
				.bestillingsId(notifikasjonMedKontaktInfoTO.getBestillingsId())
				.bestillerId(notifikasjonMedKontaktInfoTO.getBestillerId())
				.mottakerId(notifikasjonMedKontaktInfoTO.getFodselsnummer())
				.mottakerIdType(FNR)
				.status(OPPRETTET)
				.antallRenotifikasjoner(notifikasjonMedKontaktInfoTO.getAntallRenotifikasjoner())
				.renotifikasjonIntervall(notifikasjonMedKontaktInfoTO.getRenotifikasjonIntervall())
				.nesteRenotifikasjonDato(nesteRenotifikasjonDato)
				.prefererteKanaler(this.buildPrefererteKanaler(notifikasjonMedKontaktInfoTO.getPrefererteKanaler()))
				.opprettetAv(notifikasjonMedKontaktInfoTO.getBestillerId())
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
				.status(OPPRETTET)
				.kanal(kanal)
				.kontaktInfo(kontaktinformasjon)
				.tittel(tittel)
				.tekst(tekst)
				.opprettetDato(LocalDateTime.now())
				.opprettetAv(notifikasjon.getBestillingsId())
				.build();
		notifikasjon.getNotifikasjonDistribusjon().add(notifikasjonDistribusjon);
	}

	public void publishNotifikasjonDistrubisjon(Integer bestillingsId, Kanal kanal) {
		String topic = EPOST.equals(kanal) ? KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST : KAFKA_TOPIC_DOK_NOTIFKASJON_SMS;

		log.info("Publiserer bestilling med kontaktinfo til kafka topic={} med bestillingsId={}", topic, bestillingsId);
		DoknotifikasjonEpost doknotifikasjonEpostTo = new DoknotifikasjonEpost(bestillingsId);
		producer.publish(
				topic,
				doknotifikasjonEpostTo
		);
	}
}