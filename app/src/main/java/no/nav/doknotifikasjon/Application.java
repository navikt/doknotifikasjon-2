package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.alias.ServiceuserAlias;
import no.nav.doknotifikasjon.config.ApplicationConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;


@Import(value = {ApplicationConfig.class})
@SpringBootApplication
@EnableRetry
@EnableConfigurationProperties({ServiceuserAlias.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
