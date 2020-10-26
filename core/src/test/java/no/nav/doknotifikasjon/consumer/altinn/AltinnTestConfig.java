package no.nav.doknotifikasjon.consumer.altinn;

import no.altinn.springsoap.client.gen.ObjectFactory;
import no.altinn.springsoap.client.gen.SendStandaloneNotificationBasicV3;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Any;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@Configuration
public class AltinnTestConfig {

    private static final String USERNAME = "${doknotifikasjon_altinn_username}";
    private static final String PASSWORD = "${doknotifikasjon_altinn_password}";
    private static final String ALTINNURL = "${doknotifikasjon_altinn_url}";

    private ObjectFactory objectFactory = new ObjectFactory();

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
        WebServiceTemplate webServiceTemplate = Mockito.mock(WebServiceTemplate.class);

        //when(webServiceTemplate.marshalSendAndReceive(argThat(new RequestMatcher(generateRequest()))));

        AltinnConsumer client = new AltinnConsumer("username", "password");
        client.setDefaultUri("url");
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        client.setWebServiceTemplate(webServiceTemplate);
        return client;
    }

    private SendStandaloneNotificationBasicV3 generateRequest(){
        SendStandaloneNotificationBasicV3 request = objectFactory.createSendStandaloneNotificationBasicV3();

        request.setSystemUserName("username");
        request.setSystemPassword("password");

        return request;
    }

}