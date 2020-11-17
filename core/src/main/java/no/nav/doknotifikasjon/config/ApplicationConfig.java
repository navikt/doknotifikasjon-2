package no.nav.doknotifikasjon.config;

import no.nav.doknotifikasjon.metrics.DokTimedAspect;
import no.nav.doknotifikasjon.metrics.MetricService;
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
	public DokTimedAspect timedAspect(MetricService registry) {
		return new DokTimedAspect(registry);
	}

}
