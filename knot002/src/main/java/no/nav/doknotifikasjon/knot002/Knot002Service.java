package no.nav.doknotifikasjon.knot002;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.consumer.altinn.AltinnVarselConsumer;
import no.nav.doknotifikasjon.consumer.altinn.DoknotifikasjonDistributableInChannel;
import no.nav.doknotifikasjon.consumer.altinn.SmsOrEpostSenderService;
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

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SMS_UGYLDIG_KANAL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SMS_UGYLDIG_STATUS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_SMS;
import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;

@Slf4j
@Component
public class Knot002Service extends SmsOrEpostSenderService {

	private final Knot002Mapper knot002Mapper;
	private final MetricService metricService;

	public Knot002Service(Knot002Mapper knot002Mapper, KafkaEventProducer kafkaEventProducer,
						  AltinnVarselConsumer altinnVarselConsumer, NotifikasjonDistribusjonService notifikasjonDistribusjonService,
						  MetricService metricService) {
		super(kafkaEventProducer, notifikasjonDistribusjonService, altinnVarselConsumer);
		this.knot002Mapper = knot002Mapper;
		this.metricService = metricService;
	}

	public void shouldSendSms(int notifikasjonDistribusjonId) {
		send(notifikasjonDistribusjonId, Kanal.SMS, "knot002", FERDIGSTILT_NOTIFIKASJON_SMS);
	}

	@Override
	protected DoknotifikasjonDistributableInChannel mapNotifikasjonDistribusjon(NotifikasjonDistribusjon notifikasjonDistribusjon) {
		return knot002Mapper.mapNotifikasjonDistrubisjon(notifikasjonDistribusjon, notifikasjonDistribusjon.getNotifikasjon());
	}

	@Override
	protected void validateDistribusjonStatusOgKanal(DoknotifikasjonDistributableInChannel doknotifikasjonSmsObject, Notifikasjon notifikasjon) {
		if (notifikasjon.getStatus() == FERDIGSTILT) {
			throw new NotifikasjonFerdigstiltFunctionalException("Notifikasjonen har status ferdigstilt, vil avslutte utsendelsen av SMS for knot002.");
		}

		if (Status.OPPRETTET.equals(doknotifikasjonSmsObject.getDistribusjonStatus()) && Kanal.SMS.equals(doknotifikasjonSmsObject.getKanal())) {
			return;
		}

		String melding = doknotifikasjonSmsObject.getDistribusjonStatus() == Status.OPPRETTET ? FEILET_SMS_UGYLDIG_KANAL : FEILET_SMS_UGYLDIG_STATUS;
		publishStatus(doknotifikasjonSmsObject, FEILET, melding);

		log.warn("knot002 behandling av melding på kafka-topic={} avsluttes pga feil={}, bestillingsId={}", KafkaTopics.KAFKA_TOPIC_DOK_NOTIFIKASJON_SMS, melding, notifikasjon.getBestillingsId());
		throw new DoknotifikasjonValidationException(String.format("Valideringsfeil oppstod i knot002. Feilmelding: %s", melding));
	}

	@Override
	protected void registerMetricSent() {
		metricService.metricKnot002SmsSent();
	}
}
