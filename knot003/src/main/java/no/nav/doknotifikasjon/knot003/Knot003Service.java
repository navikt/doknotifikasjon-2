package no.nav.doknotifikasjon.knot003;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.consumer.altinn.AltinnVarselConsumer;
import no.nav.doknotifikasjon.consumer.altinn.DoknotifikasjonDistributableInChannel;
import no.nav.doknotifikasjon.consumer.altinn.SmsOrEpostSenderService;
import no.nav.doknotifikasjon.exception.functional.AltinnFunctionalException;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.exception.functional.NotifikasjonFerdigstiltFunctionalException;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaTopics;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonService;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_EPOST_UGYLDIG_KANAL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_EPOST_UGYLDIG_STATUS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_EPOST;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_SMS;
import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;

@Slf4j
@Component
public class Knot003Service extends SmsOrEpostSenderService {

	private final Knot003Mapper knot003Mapper;
	private final MetricService metricService;

	public Knot003Service(Knot003Mapper knot003Mapper, KafkaEventProducer kafkaEventProducer,
						  AltinnVarselConsumer altinnVarselConsumer, MetricService metricService,
						  NotifikasjonDistribusjonService notifikasjonDistribusjonService) {
		super(kafkaEventProducer, notifikasjonDistribusjonService, altinnVarselConsumer);
		this.metricService = metricService;
		this.knot003Mapper = knot003Mapper;
	}

	public void shouldSendEpost(int notifikasjonDistribusjonId) {
		send(notifikasjonDistribusjonId, Kanal.EPOST, "knot003", FERDIGSTILT_NOTIFIKASJON_EPOST);
	}

	@Override
	protected DoknotifikasjonDistributableInChannel mapNotifikasjonDistribusjon(NotifikasjonDistribusjon notifikasjonDistribusjon) {
		return knot003Mapper.mapNotifikasjonDistrubisjon(notifikasjonDistribusjon, notifikasjonDistribusjon.getNotifikasjon());
	}

	@Override
	protected void validateDistribusjonStatusOgKanal(DoknotifikasjonDistributableInChannel doknotifikasjonEpostObject, Notifikasjon notifikasjon) {
		if (notifikasjon.getStatus() == FERDIGSTILT) {
			throw new NotifikasjonFerdigstiltFunctionalException("Notifikasjonen har status ferdigstilt, vil avslutte utsendelsen av EPOST for knot003.");
		}

		if (OPPRETTET.equals(doknotifikasjonEpostObject.getDistribusjonStatus()) && Kanal.EPOST.equals(doknotifikasjonEpostObject.getKanal())) {
			return;
		}

		String melding = doknotifikasjonEpostObject.getDistribusjonStatus() == OPPRETTET ? FEILET_EPOST_UGYLDIG_KANAL : FEILET_EPOST_UGYLDIG_STATUS;
		publishStatus(doknotifikasjonEpostObject, FEILET, melding);

		log.warn("knot003 behandling av melding på kafka-topic={} avsluttes pga feil={}, bestillingsId={}", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_EPOST, melding, notifikasjon.getBestillingsId());
		throw new DoknotifikasjonValidationException(String.format("Valideringsfeil oppstod i knot003. Feilmelding: %s", melding));
	}

	@Override
	protected void registerMetricSent() {
		metricService.metricKnot003EpostSent();
	}
}
