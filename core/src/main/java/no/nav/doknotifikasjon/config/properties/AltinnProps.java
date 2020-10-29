package no.nav.doknotifikasjon.config.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;


@ConstructorBinding
@ConfigurationProperties("doknotifikasjon.altinn")
@Validated
@Getter
public class AltinnProps {

	@NotNull
	private String username;
	@NotNull
	private String password;
	@NotNull
	private String url;
	AltinnProps(String username, String password, String url) {
		this.username = username;
		this.password = password;
		this.url = url;
	}
}
