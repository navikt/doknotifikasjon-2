package no.nav.doknotifikasjon.metrics;

import io.micrometer.core.annotation.Incubating;
import io.micrometer.core.instrument.*;
import io.micrometer.core.lang.NonNullApi;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.technical.AbstractDoknotifikasjonTechnicalException;
import no.nav.doknotifikasjon.constants.MDCConstants;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;

import java.lang.reflect.Method;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static no.nav.doknotifikasjon.metrics.MetricName.DOK_EXCEPTION;
import static no.nav.doknotifikasjon.metrics.MetricTags.EXCEPTION_NAME;
import static no.nav.doknotifikasjon.metrics.MetricTags.FUNCTIONAL;
import static no.nav.doknotifikasjon.metrics.MetricTags.TECHNICAL;
import static no.nav.doknotifikasjon.metrics.MetricTags.TYPE;

@Aspect
@NonNullApi
@Incubating(since = "1.0.0")
@Slf4j
public class DokTimedAspect {

	private final MeterRegistry registry;
	private final Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinpoint;

	public DokTimedAspect(MeterRegistry registry) {
		this(registry, pjp ->
				Tags.of("class", pjp.getStaticPart().getSignature().getDeclaringTypeName(),
						"method", pjp.getStaticPart().getSignature().getName())
		);
	}

	public DokTimedAspect(MeterRegistry registry, Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinpoint) {
		this.registry = registry;
		this.tagsBasedOnJoinpoint = tagsBasedOnJoinpoint;
	}

	@Around("execution (@no.nav.doknotifikasjon.metrics.Metrics * *.*(..))")
	public Object incrementMetrics(ProceedingJoinPoint pjp) throws Throwable {
		Method method = ((MethodSignature) pjp.getSignature()).getMethod();

		Metrics metrics = method.getAnnotation(Metrics.class);
		if (metrics.value().isEmpty()) {
			return pjp.proceed();
		}

		registry.counter(metrics.value(), metrics.extraTags()).increment();

		try {
			return pjp.proceed();
		} catch (Exception e) {

			if (metrics.logExceptions()) {
				logException(method, e);
			}

			if (metrics.createErrorMetric()) {
				registry.counter(DOK_EXCEPTION,
						TYPE,
						isFunctionalException(method, e) ? FUNCTIONAL : TECHNICAL,
						EXCEPTION_NAME,
						e.getClass().getSimpleName()
				).increment();

			}

			throw e;
		}
	}

	private boolean isFunctionalException(Method method, Exception e) {
		return asList(method.getExceptionTypes()).contains(e.getClass()) || isFunctionalException(e);
	}

	private void logException(Method method, Exception e) {
		String mdcRequestId = (MDC.get(MDCConstants.MDC_REQUEST_ID) == null) ? "" : (MDC.get(MDCConstants.MDC_REQUEST_ID) + " ");

		if (isFunctionalException(method, e)) {
			log.warn(mdcRequestId + e.getMessage(), e);
		} else {
			log.error(mdcRequestId + e.getMessage(), e);
		}
	}

	private boolean isFunctionalException(Throwable e) {
		return e instanceof AbstractDoknotifikasjonTechnicalException;
	}
}
