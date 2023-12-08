package no.nav.doknotifikasjon.consumer.itest;

import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalEC2;
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

	private final INotificationAgencyExternalEC2 iNotificationAgencyExternalEC2;

	public AltinnTestConfig(INotificationAgencyExternalEC2 iNotificationAgencyExternalEC2) {
		this.iNotificationAgencyExternalEC2 = iNotificationAgencyExternalEC2;
	}

	@Bean
	public Client getClient() {
		Client client = ClientProxy.getClient(iNotificationAgencyExternalEC2);
		client.getRequestContext().put(HttpClientHTTPConduit.FORCE_HTTP_VERSION, "1.1");
		return client;
	}
}
