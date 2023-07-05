package no.nav.doknotifikasjon.config.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;


@ConfigurationProperties("doknotifikasjon.altinn")
@Validated
@Getter
public class AltinnProps {

	@NotNull
	private final String username;
	@NotNull
	private final String password;
	@NotNull
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
