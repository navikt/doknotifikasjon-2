package no.nav.doknotifikasjon.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("db")
public class DatabaseProperties {

	@NotBlank
	private String url;

	@NotBlank
	private String credentialsMountPath;

}