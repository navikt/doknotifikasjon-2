package no.nav.doknotifikasjon.consumer.altinn;

import no.altinn.springsoap.client.gen.ObjectFactory;
import no.altinn.springsoap.client.gen.SendStandaloneNotificationBasicV3;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Any;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.WebServiceMessageSender;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@Configuration
public class AltinnTestConfig {

    private ObjectFactory objectFactory = new ObjectFactory();
    private static WebServiceTemplate webServiceTemplate = Mockito.mock(WebServiceTemplate.class);

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

    public static WebServiceTemplate getWebServiceTemplateMock() {
        return webServiceTemplate;
    }

}