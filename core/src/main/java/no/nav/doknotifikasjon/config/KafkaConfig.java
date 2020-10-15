package no.nav.doknotifikasjon.config;

import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.transaction.KafkaTransactionManager;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Bean("kafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            KafkaErrorHandler errorHandler
    ) {

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConcurrency(6);
         factory.setErrorHandler(errorHandler);

        configurer.configure(factory, kafkaConsumerFactory);
        return factory;
    }
}
