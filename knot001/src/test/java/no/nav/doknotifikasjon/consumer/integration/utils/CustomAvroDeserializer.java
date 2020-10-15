package no.nav.doknotifikasjon.consumer.integration.utils;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import no.nav.doknotifikasjon.consumer.integration.itest.SerializationUtils;

public class CustomAvroDeserializer extends KafkaAvroDeserializer {

    public CustomAvroDeserializer() {
        super(SerializationUtils.REGISTRY);
    }
}
