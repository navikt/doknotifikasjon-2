package no.nav.doknotifikasjon.metrics;


import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kodeverk.Status;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.doknotifikasjon.metrics.MetricName.DOK_EXCEPTION;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT002_SMS_SENT;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT003_EPOST_SENT;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT004_CONSUMER_STATUS;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT005_RENOTIFKASJON_STOPPED;
import static no.nav.doknotifikasjon.metrics.MetricTags.*;

@Slf4j
@Component
public class MetricService {

	private final MeterRegistry meterRegistry;

	@Inject
	MetricService(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void metricKnot002SmsSent() {
		this.counter(
				DOK_KNOT002_SMS_SENT,
				TYPE,
				PROCESSED
		);
	}

	public void metricKnot003EpostSent() {
		this.counter(
				DOK_KNOT003_EPOST_SENT,
				TYPE,
				PROCESSED
		);
	}

	public void metricKnot004Status(Status status) {
		this.counter(
				DOK_KNOT004_CONSUMER_STATUS,
				STATUS,
				status.toString()
		);
	}

	public void metricKnot005ReNotifikasjonStopped() {
		this.counter(
				DOK_KNOT005_RENOTIFKASJON_STOPPED,
				TYPE,
				STOPPED
		);
	}

	public void metricHandleException(Exception e) {
		Throwable throwable = e.getCause() == null ? e : e.getCause();
		String exceptionName = throwable.getClass().getSimpleName();

		this.counter(DOK_EXCEPTION, TYPE, TECHNICAL, EXCEPTION_NAME, exceptionName);
	}

	public void counter(String name, String... tags) {
		meterRegistry.counter(name, tags).increment();
	}
}