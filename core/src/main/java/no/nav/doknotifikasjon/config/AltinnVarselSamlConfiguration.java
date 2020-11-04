package no.nav.doknotifikasjon.config;

import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.doknotifikasjon.consumer.sts.STSClientConfigurer;
import no.nav.doknotifikasjon.consumer.altinn.WsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@EnableConfigurationProperties(AltinnProps.class)
public class AltinnVarselSamlConfiguration {

    private final AltinnProps altinnProps;
    private final ServiceuserAlias serviceuserAlias;
    private final String stsUrl;

    public AltinnVarselSamlConfiguration(
            AltinnProps altinnProps,
            ServiceuserAlias serviceuserAlias,
            //TODO set property in prop class
            @Value("${security-token-service-token.url}") String stsUrl
    ) {
        this.altinnProps = altinnProps;
        this.serviceuserAlias = serviceuserAlias;
        this.stsUrl = stsUrl;
    }

    @Bean
    public INotificationAgencyExternalBasic iNotificationAgencyExternalBasic() throws URISyntaxException {
        INotificationAgencyExternalBasic port = WsClient.createPort(altinnProps.getUrl(), INotificationAgencyExternalBasic.class);
        STSClientConfigurer configurer = new STSClientConfigurer(new URI(stsUrl), serviceuserAlias.getUsername(), serviceuserAlias.getPassword());
        configurer.configureRequestSamlToken(port);
        return port;
    }
}
