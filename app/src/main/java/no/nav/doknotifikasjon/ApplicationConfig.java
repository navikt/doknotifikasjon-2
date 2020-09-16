package no.nav.doknotifikasjon;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@ComponentScan
@EnableAutoConfiguration
@EnableAspectJAutoProxy
@Configuration
public class ApplicationConfig {

//    @Bean
//    public DokTimedAspect timedAspect(MeterRegistry registry) {
//        return new DokTimedAspect(registry);
//    }

}
