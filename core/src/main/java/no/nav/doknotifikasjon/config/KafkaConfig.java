package no.nav.doknotifikasjon.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp;
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo;
import org.apache.avro.util.ClassSecurityValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;

import static org.springframework.util.backoff.FixedBackOff.DEFAULT_INTERVAL;
import static org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS;

@Slf4j
@EnableKafka
@Configuration
public class KafkaConfig {

	static {
		// Avro deserialiserer kun til klasser som er i en allowlist; alt som ikke er med her vil feile
		ClassSecurityValidator.setGlobal(ClassSecurityValidator.composite(
				ClassSecurityValidator.DEFAULT,
				ClassSecurityValidator.builder()
						.add(Doknotifikasjon.class)
						.add(DoknotifikasjonEpost.class)
						.add(DoknotifikasjonSms.class)
						.add(DoknotifikasjonStatus.class)
						.add(DoknotifikasjonStopp.class)
						.add(NotifikasjonMedkontaktInfo.class)
						.build()));
	}

	@Bean
	@Primary
	ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
			ConsumerFactory<Object, Object> kafkaConsumerFactory
	) {
		ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(kafkaConsumerFactory);
		factory.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10L));

		factory.setConcurrency(6);
		factory.setCommonErrorHandler(new DefaultErrorHandler(
				(rec, thr) -> log.error("Exception oppstått i doknotifikasjon={} kafka record til topic={}, partition={}, offset={}, UUID={} feilmelding={}",
						thr.getClass().getSimpleName(),
						rec.topic(),
						rec.partition(),
						rec.offset(),
						rec.key(),
						thr.getCause()
				),
				new FixedBackOff(DEFAULT_INTERVAL, UNLIMITED_ATTEMPTS)));
		return factory;
	}
}
