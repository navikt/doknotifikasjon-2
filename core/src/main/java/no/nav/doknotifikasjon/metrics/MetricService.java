package no.nav.doknotifikasjon.metrics;


import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.AbstractDoknotifikasjonFunctionalException;
import no.nav.doknotifikasjon.kodeverk.Status;
import org.springframework.stereotype.Component;

import static no.nav.doknotifikasjon.metrics.MetricName.DOK_EXCEPTION;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT001_BEHANDLET;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT002_SMS_SENT;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT003_EPOST_SENT;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT004_CONSUMER_STATUS;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT005_RENOTIFKASJON_STOPPED;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_KNOT006_BEHANDLET;
import static no.nav.doknotifikasjon.metrics.MetricTags.EXCEPTION_NAME;
import static no.nav.doknotifikasjon.metrics.MetricTags.FUNCTIONAL;
import static no.nav.doknotifikasjon.metrics.MetricTags.PROCESSED;
import static no.nav.doknotifikasjon.metrics.MetricTags.STATUS;
import static no.nav.doknotifikasjon.metrics.MetricTags.STOPPED;
import static no.nav.doknotifikasjon.metrics.MetricTags.TECHNICAL;
import static no.nav.doknotifikasjon.metrics.MetricTags.TYPE;

@Slf4j
@Component
public class MetricService {

	private final MeterRegistry meterRegistry;

	MetricService(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void metricKnot001RecordBehandlet() {
		this.counter(
				DOK_KNOT001_BEHANDLET
		);
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

	public void metricKnot006RecordBehandlet() {
		this.counter(
				DOK_KNOT006_BEHANDLET
		);
	}

	public void metricHandleException(
			Class<? extends Throwable>[] include,
			Class<? extends Throwable>[] exclude,
			Exception e
	) {
		if (include.length > 0 && !this.existInList(include, e)) {
			return;
		}

		if (exclude.length > 0 && this.existInList(exclude, e)) {
			return;
		}

		metricHandleException(e);
	}

	public boolean existInList(
			Class<? extends Throwable>[] list,
			Exception e
	) {
		boolean existInList = false;

		for (Class<? extends Throwable> i : list) {
			if (e.getClass().equals(i)) {
				existInList = true;
				break;
			}
		}

		return existInList;
	}

	public void metricHandleException(Exception e) {
		Throwable throwable = e.getCause() == null ? e : e.getCause();
		String exceptionName = throwable.getClass().getSimpleName();

		this.counter(
				DOK_EXCEPTION,
				TYPE,
				isFunctionalException(e) ? FUNCTIONAL : TECHNICAL,
				EXCEPTION_NAME,
				exceptionName
		);
	}

	private boolean isFunctionalException(Throwable e) {
		return e instanceof AbstractDoknotifikasjonFunctionalException;
	}

	public void counter(String name, String... tags) {
		meterRegistry.counter(name, tags).increment();
	}
}