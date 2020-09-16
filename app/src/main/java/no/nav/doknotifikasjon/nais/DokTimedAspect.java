package no.nav.doknotifikasjon.nais;

import io.micrometer.core.annotation.Incubating;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.lang.NonNullApi;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;

import java.util.function.Function;

@Aspect
@NonNullApi
@Incubating(since = "1.0.0")
@Slf4j
public class DokTimedAspect {
    public DokTimedAspect(MeterRegistry registry) {

    }
    public DokTimedAspect() {}

    public DokTimedAspect(MeterRegistry registry, Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinpoint) {
    }
}
