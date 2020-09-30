package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Import(value = {KafkaTestConfig.class})
@Profile("itest")
public class ApplicationTestConfig {}
