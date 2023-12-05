package no.nav.doknotifikasjon.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("virksomhetssertifikat")
public record KeyStoreProperties(@NotBlank String path,
								 @NotBlank String password,
								 @NotBlank String alias) {
}
