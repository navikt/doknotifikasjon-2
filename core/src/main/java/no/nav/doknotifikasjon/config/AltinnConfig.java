package no.nav.doknotifikasjon.config;

import no.nav.doknotifikasjon.consumer.altinn.AltinnConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
public class AltinnConfig {

    private static final String USERNAME = "${doknotifikasjon_altinn_username}";
    private static final String PASSWORD = "${doknotifikasjon_altinn_password}";
    private static final String ALTINNURL = "${doknotifikasjon_altinn_url}";


    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("no.altinn.springsoap.client.gen");
        return marshaller;
    }

    @Bean
    public AltinnConsumer altinnClient(
            Jaxb2Marshaller marshaller,
            @Value(USERNAME) final String username,
            @Value(PASSWORD) final String password,
            @Value(ALTINNURL) final String url
    ) {
        AltinnConsumer client = new AltinnConsumer(username, password);
        client.setDefaultUri(url);
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        client.setUsername(username);
        client.setPassword(password);

        return client;
    }

}