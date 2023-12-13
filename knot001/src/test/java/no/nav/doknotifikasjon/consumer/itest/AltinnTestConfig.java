package no.nav.doknotifikasjon.consumer.itest;

import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HttpClientHTTPConduit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Profile("itest")
@Configuration
@ComponentScan
public class AltinnTestConfig {

	private final INotificationAgencyExternalBasic iNotificationAgencyExternalBasic;

	public AltinnTestConfig(INotificationAgencyExternalBasic iNotificationAgencyExternalBasic) {
		this.iNotificationAgencyExternalBasic = iNotificationAgencyExternalBasic;
	}

	@Bean
	public Client getClient() {
		Client client = ClientProxy.getClient(iNotificationAgencyExternalBasic);
		client.getRequestContext().put(HttpClientHTTPConduit.FORCE_HTTP_VERSION, "1.1");
		return client;
	}
}
