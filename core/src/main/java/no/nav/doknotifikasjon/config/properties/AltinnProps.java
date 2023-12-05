package no.nav.doknotifikasjon.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("doknotifikasjon.altinn")
public record AltinnProps(
		@NotNull
		String username,
		@NotNull
		String password,
		@NotNull
		String endpoint, boolean altinnlogg) {
}
