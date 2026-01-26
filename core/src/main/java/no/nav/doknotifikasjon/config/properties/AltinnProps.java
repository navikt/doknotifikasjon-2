package no.nav.doknotifikasjon.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@ConfigurationProperties("doknotifikasjon.altinn")
@Validated
@Getter
public class AltinnProps {

	@NotBlank
	private final String username;
	@NotBlank
	private final String password;
	@NotBlank
	private final String url;

	AltinnProps(String username, String password, String url) {
		this.username = username;
		this.password = password;
		this.url = url;
	}

	@Override
	public String toString() {
		return url;
	}
}
