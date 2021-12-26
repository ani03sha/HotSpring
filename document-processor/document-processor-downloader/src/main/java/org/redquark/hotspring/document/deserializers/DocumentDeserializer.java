package org.redquark.hotspring.document.deserializers;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.kafka.common.serialization.Deserializer;
import org.redquark.hotspring.document.domains.DocumentBatch;

import java.util.Map;

public class DocumentDeserializer<T extends DocumentBatch> implements Deserializer<DocumentBatch> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }

    @Override
    public T deserialize(String s, byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("Received data is null");
        }
        return SerializationUtils.deserialize(bytes);
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }
}
