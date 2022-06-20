package no.nav.doknotifikasjon.config;

import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.doknotifikasjon.consumer.sts.STSConfig;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(AltinnProps.class)
public class AltinnVarselSamlConfiguration {

    @Bean
    public INotificationAgencyExternalBasic iNotificationAgencyExternalBasic(AltinnProps altinnProps, STSConfig stsConfig) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(INotificationAgencyExternalBasic.class);
        factory.setAddress(Objects.requireNonNull(altinnProps.getUrl()));
        factory.getFeatures().add(new WSAddressingFeature());
        INotificationAgencyExternalBasic iNotificationAgencyExternalBasic = (INotificationAgencyExternalBasic) factory.create();
        stsConfig.configureSTS(iNotificationAgencyExternalBasic);
        Client client = ClientProxy.getClient(iNotificationAgencyExternalBasic);
        setClientTimeout(client);
        return iNotificationAgencyExternalBasic;
    }

    private void setClientTimeout(Client client) {
        HTTPConduit conduit = (HTTPConduit) client.getConduit();
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(TimeUnit.SECONDS.toMillis(2));
        httpClientPolicy.setReceiveTimeout(TimeUnit.SECONDS.toMillis(20));
        conduit.setClient(httpClientPolicy);
    }
}
