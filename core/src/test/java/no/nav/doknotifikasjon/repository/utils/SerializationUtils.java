package no.nav.doknotifikasjon.repository.utils;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;

public class SerializationUtils {
    public static final SchemaRegistryClient REGISTRY = new MockSchemaRegistryClient();

    private SerializationUtils() {}
}