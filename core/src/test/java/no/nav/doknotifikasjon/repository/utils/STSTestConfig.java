package no.nav.doknotifikasjon.repository.utils;

import no.nav.doknotifikasjon.config.ServiceuserAlias;
import no.nav.doknotifikasjon.consumer.sts.STSConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Component
@Profile("itest")
public class STSTestConfig extends STSConfig {

	public STSTestConfig(@Value("${security-token-service-saml-token.url}") String stsUrl, final ServiceuserAlias serviceuserAlias) {
		super(stsUrl, serviceuserAlias);
	}

	@Override
	public void configureSTS(Object port){

	}
	
}
