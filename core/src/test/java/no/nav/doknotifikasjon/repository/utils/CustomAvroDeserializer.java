package no.nav.doknotifikasjon.repository.utils;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;

public class CustomAvroDeserializer extends KafkaAvroDeserializer {

	public CustomAvroDeserializer() {
		super(SerializationUtils.REGISTRY);
	}
}
