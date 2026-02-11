package no.nav.doknotifikasjon.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

@Validated
@Profile("altinn3")
@ConfigurationProperties("altinn3")
public record Altinn3Props(@NotBlank String altinnTokenExchangeUri, @NotBlank String notificationOrderUri) {
}
