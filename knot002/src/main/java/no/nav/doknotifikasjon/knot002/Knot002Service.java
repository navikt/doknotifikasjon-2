package no.nav.doknotifikasjon.knot002;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.consumer.altinn.AltinnVarselConsumer;
import no.nav.doknotifikasjon.consumer.altinn.SmsOrEpostSenderService;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.metrics.MetricService;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonService;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SMS_UGYLDIG_KANAL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_SMS_UGYLDIG_STATUS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_SMS;

@Slf4j
@Component
public class Knot002Service extends SmsOrEpostSenderService<DoknotifikasjonSmsObject> {

	private final Knot002Mapper knot002Mapper;
	private final AltinnVarselConsumer altinnVarselConsumer;

	public Knot002Service(Knot002Mapper knot002Mapper, KafkaEventProducer kafkaEventProducer,
						  AltinnVarselConsumer altinnVarselConsumer, NotifikasjonDistribusjonService notifikasjonDistribusjonService, MetricService metricService) {
		super(kafkaEventProducer, notifikasjonDistribusjonService, metricService);
		this.knot002Mapper = knot002Mapper;
		this.altinnVarselConsumer = altinnVarselConsumer;
	}

	public void sendSms(int notifikasjonDistribusjonId) {
		send(notifikasjonDistribusjonId, Kanal.SMS, "knot002");
	}

	@Override
	protected Optional<UUID> sendVarselToKanal(String bestillingsId, DoknotifikasjonSmsObject doknotifikasjonSmsObject) {
		return altinnVarselConsumer.sendSmsVarsel(bestillingsId, doknotifikasjonSmsObject.getKontaktInfo(), doknotifikasjonSmsObject.getFodselsnummer(), doknotifikasjonSmsObject.getTekst());
	}

	@Override
	protected DoknotifikasjonSmsObject mapNotifikasjonDistribusjon(NotifikasjonDistribusjon notifikasjonDistribusjon) {
		return knot002Mapper.mapNotifikasjonDistribusjon(notifikasjonDistribusjon, notifikasjonDistribusjon.getNotifikasjon());
	}

	@Override
	protected String messageSuccessNotifikasjonStatus() {
		return FERDIGSTILT_NOTIFIKASJON_SMS;
	}

	@Override
	protected String messageInvalidDistribusjonStatusMessage() {
		return FEILET_SMS_UGYLDIG_STATUS;
	}

	@Override
	protected String messageInvalidChannelMessage() {
		return FEILET_SMS_UGYLDIG_KANAL;
	}
}
