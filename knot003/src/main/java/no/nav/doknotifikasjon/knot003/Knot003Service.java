package no.nav.doknotifikasjon.knot003;

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

import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_EPOST_UGYLDIG_KANAL;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_EPOST_UGYLDIG_STATUS;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FERDIGSTILT_NOTIFIKASJON_EPOST;

@Slf4j
@Component
public class Knot003Service extends SmsOrEpostSenderService<DoknotifikasjonEpostObject> {

	private final Knot003Mapper knot003Mapper;
	private final AltinnVarselConsumer altinnVarselConsumer;

	public Knot003Service(Knot003Mapper knot003Mapper, KafkaEventProducer kafkaEventProducer,
						  AltinnVarselConsumer altinnVarselConsumer,
						  NotifikasjonDistribusjonService notifikasjonDistribusjonService, MetricService metricService) {
		super(kafkaEventProducer, notifikasjonDistribusjonService, metricService);
		this.knot003Mapper = knot003Mapper;
		this.altinnVarselConsumer = altinnVarselConsumer;
	}

	public void sendEpost(int notifikasjonDistribusjonId) {
		send(notifikasjonDistribusjonId, Kanal.EPOST, "knot003");
	}

	@Override
	protected Optional<UUID> sendVarselToKanal(String bestillingsId, DoknotifikasjonEpostObject doknotifikasjonEpostObject) {
		return altinnVarselConsumer.sendEpostVarsel(bestillingsId, doknotifikasjonEpostObject.getKontaktInfo(), doknotifikasjonEpostObject.getFodselsnummer(), doknotifikasjonEpostObject.getTekst(), doknotifikasjonEpostObject.getTittel());
	}

	@Override
	protected DoknotifikasjonEpostObject mapNotifikasjonDistribusjon(NotifikasjonDistribusjon notifikasjonDistribusjon) {
		return knot003Mapper.mapNotifikasjonDistribusjon(notifikasjonDistribusjon, notifikasjonDistribusjon.getNotifikasjon());
	}

	@Override
	protected String messageSuccessNotifikasjonStatus() {
		return FERDIGSTILT_NOTIFIKASJON_EPOST;
	}

	@Override
	protected String messageInvalidDistribusjonStatusMessage() {
		return FEILET_EPOST_UGYLDIG_STATUS;
	}

	@Override
	protected String messageInvalidChannelMessage() {
		return FEILET_EPOST_UGYLDIG_KANAL;
	}
}
