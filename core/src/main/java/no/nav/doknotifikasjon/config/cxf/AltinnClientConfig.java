package no.nav.doknotifikasjon.config.cxf;

import jakarta.xml.ws.BindingProvider;
import no.altinn.services.serviceengine.notification._2010._10.INotificationAgencyExternalEC2;
import no.altinn.services.serviceengine.notification._2010._10.NotificationAgencyExternalEC2SF;
import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.doknotifikasjon.config.properties.KeyStoreProperties;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;
import java.util.Set;

import static jakarta.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;
import static jakarta.xml.ws.BindingProvider.SESSION_MAINTAIN_PROPERTY;
import static org.apache.cxf.rt.security.SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT;
import static org.apache.cxf.rt.security.SecurityConstants.STS_ISSUE_AFTER_FAILED_RENEW;
import static org.apache.cxf.rt.security.SecurityConstants.STS_TOKEN_IMMINENT_EXPIRY_VALUE;

@Configuration
public class AltinnClientConfig {

	@Bean
	public INotificationAgencyExternalEC2 iNotificationAgencyExternalEC2(AltinnProps altinnProps,
																		 Bus bus,
																		 KeyStoreProperties keyStoreProperties) {
		NotificationAgencyExternalEC2SF service = new NotificationAgencyExternalEC2SF();
		INotificationAgencyExternalEC2 port = service.getCustomBindingINotificationAgencyExternalEC2();
		BindingProvider bindingProvider = (BindingProvider) port;
		bindingProvider.getRequestContext().put(ENDPOINT_ADDRESS_PROPERTY, altinnProps.endpoint());

		Client client = getClient(port, keyStoreProperties);

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

	public Client getClient(INotificationAgencyExternalEC2 port, KeyStoreProperties keyStoreProperties) {
		Client client = ClientProxy.getClient(port);
		client.getRequestContext().put("security.signature.properties", getKeyStoreProperties(keyStoreProperties));
		client.getRequestContext().put("security.must-understand", true);
		client.getRequestContext().put("org.apache.cxf.message.Message.MAINTAIN_SESSION", true);
		client.getRequestContext().put(SESSION_MAINTAIN_PROPERTY, true);
		client.getRequestContext().put(CACHE_ISSUED_TOKEN_IN_ENDPOINT, true);
		client.getRequestContext().put(STS_ISSUE_AFTER_FAILED_RENEW, true);
		client.getRequestContext().put(STS_TOKEN_IMMINENT_EXPIRY_VALUE, 15);
		return client;
	}

	private Properties getKeyStoreProperties(KeyStoreProperties keyStoreProperties) {
		final Properties properties = new Properties();
		properties.setProperty("org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin");
		properties.setProperty("org.apache.ws.security.crypto.merlin.keystore.file", keyStoreProperties.path());
		properties.setProperty("org.apache.ws.security.crypto.merlin.keystore.password", keyStoreProperties.password());
		properties.setProperty("org.apache.ws.security.crypto.merlin.keystore.type", "pkcs12");
		properties.setProperty("org.apache.ws.security.crypto.merlin.keystore.private.password", keyStoreProperties.password());
		properties.setProperty("org.apache.ws.security.crypto.merlin.keystore.alias", keyStoreProperties.alias());
		return properties;
	}
}
