package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.config.ServiceuserAlias;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableConfigurationProperties({ServiceuserAlias.class})
public class CoreConfig {

}
