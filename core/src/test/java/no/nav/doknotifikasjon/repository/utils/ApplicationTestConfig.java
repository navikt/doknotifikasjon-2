package no.nav.doknotifikasjon.repository.utils;


import no.nav.doknotifikasjon.CoreConfig;
import no.nav.doknotifikasjon.LocalTestCacheConfig;
import no.nav.doknotifikasjon.repository.RepositoryConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
		CoreConfig.class,
		RepositoryConfig.class,
		LocalTestCacheConfig.class
})
public class ApplicationTestConfig {
}
