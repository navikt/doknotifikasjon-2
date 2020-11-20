package no.nav.doknotifikasjon.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

class MetricServiceTest {

	@Mock
	private MeterRegistry meterRegistry;


	@Test
	void serviceShouldReturnTrueWhenExceptionIsInList() {
		MetricService metricService = new MetricService(meterRegistry);
		InterruptedException interruptedException = new InterruptedException();
		Class<? extends Throwable>[] errorList = new Class[]{InterruptedException.class};

		assertTrue(metricService.existInList(errorList, interruptedException));
	}

	@Test
	void serviceShouldReturnFalseWhenListIsEmpty() {
		MetricService metricService = new MetricService(meterRegistry);
		InterruptedException interruptedException = new InterruptedException();
		Class<? extends Throwable>[] errorList = new Class[]{};

		assertFalse(metricService.existInList(errorList, interruptedException));
	}

	@Test
	void serviceShouldReturnFalseWhenExceptionIsNotInList() {
		MetricService metricService = new MetricService(meterRegistry);
		InterruptedException interruptedException = new InterruptedException();
		Class<? extends Throwable>[] errorList = new Class[]{Exception.class, NullPointerException.class};

		assertFalse(metricService.existInList(errorList, interruptedException));
	}
}
