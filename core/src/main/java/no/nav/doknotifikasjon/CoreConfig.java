package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.config.AzureConfig;
import no.nav.doknotifikasjon.config.DigdirKrrProxyConfig;
import no.nav.doknotifikasjon.config.ServiceuserAlias;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@ComponentScan
@EnableConfigurationProperties({
		ServiceuserAlias.class,
		AzureConfig.class,
		DigdirKrrProxyConfig.class,
		AltinnProps.class
})
@EnableRetry
@EnableJwtTokenValidation(ignore = {"org.springframework", "org.springdoc"})
public class CoreConfig {

}
