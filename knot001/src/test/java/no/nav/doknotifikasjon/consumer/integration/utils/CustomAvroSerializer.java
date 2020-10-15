package no.nav.doknotifikasjon.consumer.integration.utils;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import no.nav.doknotifikasjon.consumer.integration.itest.SerializationUtils;

public class CustomAvroSerializer extends KafkaAvroSerializer {
    public CustomAvroSerializer() {
        super(SerializationUtils.REGISTRY);
    }
}
