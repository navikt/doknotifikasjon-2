package no.nav.doknotifikasjon.utils;

import io.confluent.kafka.serializers.KafkaAvroSerializer;

public class CustomAvroSerializer extends KafkaAvroSerializer {
    public CustomAvroSerializer() {
        super(SerializationUtils.REGISTRY);
    }
}
