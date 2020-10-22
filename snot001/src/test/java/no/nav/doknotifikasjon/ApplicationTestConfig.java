package no.nav.doknotifikasjon;


import no.nav.doknotifikasjon.repository.RepositoryConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        CoreConfig.class,
        RepositoryConfig.class
})
public class ApplicationTestConfig {
}
