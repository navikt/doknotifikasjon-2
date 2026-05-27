package no.nav.doknotifikasjon.repository;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = {"no.nav.doknotifikasjon.model"})
@EnableJpaRepositories(basePackageClasses = {
		NotifikasjonRepository.class,
		NotifikasjonDistribusjonRepository.class})
@Configuration
@Profile("itest")
public class RepositoryConfig {
}
