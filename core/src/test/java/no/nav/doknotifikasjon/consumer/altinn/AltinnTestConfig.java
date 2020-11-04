package no.nav.doknotifikasjon.consumer.altinn;

import no.nav.doknotifikasjon.consumer.altinn.old.AltinnConsumer;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

@Configuration
public class AltinnTestConfig {
/*
	private static WebServiceTemplate webServiceTemplate = Mockito.mock(WebServiceTemplate.class);
	private ObjectFactory objectFactory = new ObjectFactory();

	public static WebServiceTemplate getWebServiceTemplateMock() {
		return webServiceTemplate;
	}

	@Bean
	public Jaxb2Marshaller marshaller() {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath("no.altinn.springsoap.client.gen");
		return marshaller;
	}

	@Bean
	public AltinnConsumer altinnConsumer(
			Jaxb2Marshaller marshaller
	) {
		AltinnConsumer client = new AltinnConsumer("username", "password");
		client.setDefaultUri("localhost");
		client.setMarshaller(marshaller);
		client.setUnmarshaller(marshaller);
		client.setWebServiceTemplate(webServiceTemplate);
		return client;

	}

	private SendStandaloneNotificationBasicV3 generateRequest() {
		SendStandaloneNotificationBasicV3 request = objectFactory.createSendStandaloneNotificationBasicV3();

		request.setSystemUserName("username");
		request.setSystemPassword("password");

		return request;
	}

 */

}