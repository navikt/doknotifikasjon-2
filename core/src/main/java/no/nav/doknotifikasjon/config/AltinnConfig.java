package no.nav.doknotifikasjon.config;

import no.nav.doknotifikasjon.config.properties.AltinnProps;
import no.nav.doknotifikasjon.consumer.altinn.AltinnConsumer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
@EnableConfigurationProperties(AltinnProps.class)
public class AltinnConfig {

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("no.altinn.springsoap.client.gen");
        return marshaller;
    }

    @Bean
    public AltinnConsumer altinnConsumer(
            Jaxb2Marshaller marshaller,
            AltinnProps altinnProps
    ) {
        AltinnConsumer client = new AltinnConsumer(altinnProps.getUsername(), altinnProps.getPassword());
        client.setDefaultUri(altinnProps.getUrl());
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);

        return client;
    }

}