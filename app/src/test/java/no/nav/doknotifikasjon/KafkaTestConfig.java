package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.utils.KafkaTopics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;

@EnableKafka
@Configuration
@Profile("itest")
public class KafkaTestConfig {

    @Bean
    @Order(1)
    public EmbeddedKafkaRule kafkaEmbedded() {
        EmbeddedKafkaRule embedded = new EmbeddedKafkaRule(1, true, 1, KafkaTopics.KAFKA_TOPIC_DOK_EKSTERN_NOTIFKASJON);
        embedded.kafkaPorts(60172);
        embedded.brokerProperty("offsets.topic.replication.factor", (short) 1);
        embedded.brokerProperty("transaction.state.log.replication.factor", (short) 1);
        embedded.brokerProperty("transaction.state.log.min.isr", 1);
        return embedded;
    }
}
