package no.nav.doknotifikasjon.consumer.altinn;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ws.client.core.WebServiceTemplate;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AltinnTestConfig.class})
@ActiveProfiles("itest")
public class AltinnConsumerTest {

    @Autowired
    Jaxb2Marshaller marshaller;



    private AltinnConsumer generateAltinnConsumer(){
        WebServiceTemplate webServiceTemplate = Mockito.mock(WebServiceTemplate.class);

        //when(webServiceTemplate.marshalSendAndReceive(argThat(new RequestMatcher(generateRequest()))));

        AltinnConsumer client = new AltinnConsumer("username", "password");
        client.setDefaultUri("url");
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        client.setWebServiceTemplate(webServiceTemplate);
        return client;
    }
}