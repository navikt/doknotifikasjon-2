package no.nav.doknotifikasjon.metrics;

import io.micrometer.core.annotation.Incubating;
import io.micrometer.core.lang.NonNullApi;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.constants.MDCConstants;
import no.nav.doknotifikasjon.exception.functional.AbstractDoknotifikasjonFunctionalException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;

import java.lang.reflect.Method;

import static java.util.Arrays.asList;

@Aspect
@NonNullApi
@Incubating(since = "1.0.0")
@Slf4j
public class DokTimedAspect {

	private final MetricService registry;

	public DokTimedAspect(MetricService registry) {
		this.registry = registry;
	}

	@Around("execution (@no.nav.doknotifikasjon.metrics.Metrics * *.*(..))")
	public Object incrementMetrics(ProceedingJoinPoint pjp) throws Throwable {
		Method method = ((MethodSignature) pjp.getSignature()).getMethod();

		Metrics metrics = method.getAnnotation(Metrics.class);
		if (metrics.value().isEmpty() && !metrics.createErrorMetric()) {
			return pjp.proceed();
		}

		if (!metrics.value().isEmpty()) {
			registry.counter(metrics.value(), metrics.extraTags());
		}

		try {
			return pjp.proceed();
		} catch (Exception e) {

			if (metrics.logExceptions()) {
				logException(method, e);
			}

			if (metrics.createErrorMetric()) {
				registry.metricHandleException(metrics.errorMetricInclude(), metrics.errorMetricExclude(), e);
			}

			throw e;
		}
	}

	private void logException(Method method, Exception e) {
		String mdcRequestId = (MDC.get(MDCConstants.MDC_REQUEST_ID) == null) ? "" : (MDC.get(MDCConstants.MDC_REQUEST_ID) + " ");

		if (isFunctionalException(method, e)) {
			log.warn(mdcRequestId + e.getMessage(), e);
		} else {
			log.error(mdcRequestId + e.getMessage(), e);
		}
	}

	private boolean isFunctionalException(Method method, Exception e) {
		return asList(method.getExceptionTypes()).contains(e.getClass()) || isFunctionalException(e);
	}

	private boolean isFunctionalException(Throwable e) {
		return e instanceof AbstractDoknotifikasjonFunctionalException;
	}
}
