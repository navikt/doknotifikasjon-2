package no.nav.doknotifikasjon.config.properties;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("postgres.db")
public class PostgresProperties {

	@NotEmpty
	private String url;

	@NotEmpty
	private String credentialsMountPath;

}