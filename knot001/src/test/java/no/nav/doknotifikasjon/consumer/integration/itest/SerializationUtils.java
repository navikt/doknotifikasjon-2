package no.nav.doknotifikasjon.consumer.integration.itest;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;

public class SerializationUtils {
    public static final SchemaRegistryClient REGISTRY = new MockSchemaRegistryClient();

    private SerializationUtils() {}
}