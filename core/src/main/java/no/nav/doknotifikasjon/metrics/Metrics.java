package no.nav.doknotifikasjon.metrics;

import java.lang.annotation.*;

@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Metrics {
	String value() default "";

	String[] extraTags() default {};

	double[] percentiles() default {};

	String description() default "";

	boolean histogram() default false;

	boolean logExceptions() default true;

	boolean createErrorMetric() default false;

	Class<? extends Throwable>[] errorMetricExclude() default {};

	Class<? extends Throwable>[] errorMetricInclude() default {};
}