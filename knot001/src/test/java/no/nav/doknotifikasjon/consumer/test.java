package no.nav.doknotifikasjon.consumer;


import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_EKSTERN_NOTIFKASJON;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

import javax.net.ServerSocketFactory;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit4.SpringRunner;


//@RunWith(SpringRunner.class)
//public class test {
//
//    private static final String TEST_EMBEDDED = KAFKA_TOPIC_DOK_EKSTERN_NOTIFKASJON;
//
//    @Autowired
//    private Config config;
//
//    @Autowired
//    private EmbeddedKafkaBroker broker;
//
//    @Autowired
//    private KafkaEventConsumer kafkaEventConsumer;
//
//    @Test
//    public void testLateStartedConsumer() throws Exception {
//        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(TEST_EMBEDDED, "false", this.broker);
//        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//
//        Producer<String, Object> producer = new KafkaProducer<>(KafkaTestUtils.producerProps(this.broker));
//        producer.send(new ProducerRecord<>(TEST_EMBEDDED, "foo"));
//        producer.close();
//    }
//
//    @Configuration
//    public static class Config {
//
//        private int port;
//
//        @Bean
//        public EmbeddedKafkaBroker broker() throws IOException {
//            ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket(0);
//            this.port = ss.getLocalPort();
//            ss.close();
//
//            return new EmbeddedKafkaBroker(1, true, TEST_EMBEDDED)
//                    .kafkaPorts(this.port);
//        }
//
//    }
//}