package no.nav.doknotifikasjon.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("altinn3")
public record Altinn3Props(@NotBlank String altinnTokenExchangeUri, @NotBlank String notificationOrderUri) {
}
