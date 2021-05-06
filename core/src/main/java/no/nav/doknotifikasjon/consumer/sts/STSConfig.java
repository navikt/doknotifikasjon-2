package no.nav.doknotifikasjon.consumer.sts;

import no.nav.doknotifikasjon.config.ServiceuserAlias;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Profile({"nais", "local"})
public class STSConfig {

    private final String stsUrl;
    private final ServiceuserAlias serviceuserAlias;

    @Inject
    public STSConfig(@Value("${security-token-service-saml-token.url}") String stsUrl, final ServiceuserAlias serviceuserAlias) {
        this.stsUrl = stsUrl;
        this.serviceuserAlias = serviceuserAlias;
    }

    public void configureSTS(Object port) {
        Client client = ClientProxy.getClient(port);
        STSConfigUtil.configureStsRequestSamlToken(client, stsUrl, serviceuserAlias.getUsername(), serviceuserAlias.getPassword());
    }
}
