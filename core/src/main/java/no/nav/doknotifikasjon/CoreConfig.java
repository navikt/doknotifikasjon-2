package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.config.ServiceuserAlias;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@ComponentScan
@EnableConfigurationProperties({ServiceuserAlias.class})
@EnableRetry
public class CoreConfig {

}
