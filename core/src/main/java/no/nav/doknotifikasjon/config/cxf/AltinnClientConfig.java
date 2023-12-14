package no.nav.doknotifikasjon.config.cxf;

import jakarta.xml.ws.BindingProvider;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalBasic;
import no.altinn.services.serviceengine.notification._2010._10.NotificationAgencyExternalBasicSF;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

import static jakarta.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;
import static java.lang.Boolean.TRUE;

@Configuration
public class AltinnClientConfig {

	@Bean
	public INotificationAgencyExternalBasic INotificationAgencyExternalBasic(AltinnProps altinnProps,
																			 Bus bus) {
		NotificationAgencyExternalBasicSF service = new NotificationAgencyExternalBasicSF();
		INotificationAgencyExternalBasic port = service.getBasicHttpBindingINotificationAgencyExternalBasic();
		BindingProvider bindingProvider = (BindingProvider) port;
		bindingProvider.getRequestContext().put(ENDPOINT_ADDRESS_PROPERTY, altinnProps.endpoint());

		Client client = getClient(port, altinnProps);

		if (altinnProps.altinnlogg()) {
			client.getInInterceptors().add(new LoggingInInterceptor());
			LoggingOutInterceptor outInterceptor = new LoggingOutInterceptor();
			outInterceptor.setSensitiveElementNames(Set.of("ns2:systemPassword"));
			outInterceptor.setPrettyLogging(true);
			outInterceptor.setLimit(1024 * 1024 * 100);
			client.getOutInterceptors().add(outInterceptor);
			client.getInFaultInterceptors().add(new LoggingInInterceptor());
		}
		return port;
	}

	public Client getClient(INotificationAgencyExternalBasic port, AltinnProps altinnProps) {
		Client client = ClientProxy.getClient(port);
		client.getRequestContext().put("ws-security.must-understand", TRUE);
		client.getRequestContext().put("ws-security.username", altinnProps.username());
		client.getRequestContext().put("ws-security.callback-handler", new AltinnClientCallBackHandler(altinnProps));
		client.getRequestContext().put("org.apache.cxf.message.Message.MAINTAIN_SESSION", TRUE);
		client.getRequestContext().put("jakarta.xml.ws.session.maintain", TRUE);
		return client;
	}
}
