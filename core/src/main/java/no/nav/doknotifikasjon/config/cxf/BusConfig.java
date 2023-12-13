package no.nav.doknotifikasjon.config.cxf;

import no.nav.doknotifikasjon.config.cxf.interceptor.BadContextTokenInFaultInterceptor;
import no.nav.doknotifikasjon.config.cxf.interceptor.CookiesInInterceptor;
import no.nav.doknotifikasjon.config.cxf.interceptor.HeaderInterceptor;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusConfig {
	/**
	 * Initialiserer CFX Bus med nødvendige interceptors og logging.
	 *
	 * @return Bus
	 */
	@Bean
	public Bus springBus(AltinnProps altinnProps) {
		SpringBus bus = new SpringBus();
		bus.getInInterceptors().add(new CookiesInInterceptor());
		bus.getInFaultInterceptors().add(new LoggingInInterceptor());
		bus.getOutInterceptors().add(new HeaderInterceptor());

		bus.getInFaultInterceptors().add(new BadContextTokenInFaultInterceptor());

		if (altinnProps.altinnlogg()) {
			bus.getInInterceptors().add(new LoggingInInterceptor());
			bus.getInFaultInterceptors().add(new LoggingInInterceptor());
			bus.getOutInterceptors().add(new LoggingOutInterceptor());
		}
		return bus;
	}
}

