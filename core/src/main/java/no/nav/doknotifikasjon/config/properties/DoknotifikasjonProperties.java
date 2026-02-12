package no.nav.doknotifikasjon.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties("doknotifikasjon")
public class DoknotifikasjonProperties {

	@Valid
	private final SlackProperties slack = new SlackProperties();

	@Data
	public static class SlackProperties {
		@NotBlank
		@ToString.Exclude
		private String token;
		@NotBlank
		private String channel;
		private boolean enabled;
	}
}
