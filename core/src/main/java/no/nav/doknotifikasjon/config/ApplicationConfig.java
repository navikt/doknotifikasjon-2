package no.nav.doknotifikasjon.config;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.doknotifikasjon.metrics.DokTimedAspect;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan
@EnableAutoConfiguration
@EnableAspectJAutoProxy
@EnableScheduling
@Configuration
public class ApplicationConfig {

    @Bean
    public DokTimedAspect timedAspect(MeterRegistry registry) {
        return new DokTimedAspect(registry);
    }

}
