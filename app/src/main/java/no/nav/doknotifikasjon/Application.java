package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.knot002.Knot002Config;
import no.nav.doknotifikasjon.knot003.Knot003Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;


@Import(value = {CoreConfig.class, Knot001Config.class, Knot002Config.class, Knot003Config.class})
@SpringBootApplication
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
