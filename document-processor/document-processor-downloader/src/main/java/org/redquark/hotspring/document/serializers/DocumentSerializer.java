package org.redquark.hotspring.document.serializers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.kafka.common.serialization.Serializer;

import java.io.Serializable;
import java.util.Map;

@Slf4j
public class DocumentSerializer<T extends Serializable> implements Serializer<T> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Serializer.super.configure(configs, isKey);
    }

    @Override
    public byte[] serialize(String s, T data) {
        if (data == null) {
            throw new NullPointerException("Document to be serialized is NULL");
        }
        log.info("Serializing document...");
        return SerializationUtils.serialize(data);
    }

    @Override
    public void close() {
        Serializer.super.close();
    }
}
