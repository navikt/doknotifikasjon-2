package no.nav.doknotifikasjon.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import javax.validation.constraints.NotNull;

@ConstructorBinding
@ConfigurationProperties("doknotifikasjon.altinn")
public class AltinnProps {

	@NotNull
	private String username;
	@NotNull
	private String password;
	@NotNull
	private String url;
	AltinnProps(String username, String password, String url){
		this.username = username;
		this.password = password;
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getUrl() {
		return url;
	}
}
