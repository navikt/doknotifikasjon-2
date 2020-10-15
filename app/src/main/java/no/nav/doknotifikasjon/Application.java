package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.config.ApplicationConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;


@Import(value = CoreConfig.class)
@SpringBootApplication
@EnableRetry // TODO can be moved to CoreConfig
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
