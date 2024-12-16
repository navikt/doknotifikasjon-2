package no.nav.doknotifikasjon.config.properties;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("db")
public class DatabaseProperties {

	@NotEmpty
	private String url;

	@NotEmpty
	private String credentialsMountPath;

}