package org.redquark.hotspring.document.producers;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.redquark.hotspring.document.domains.DocumentBatch;
import org.redquark.hotspring.document.serializers.DocumentSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.CLIENT_ID_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@Slf4j
public class DocumentKafkaProducer {

    private static volatile DocumentKafkaProducer instance;
    private final KafkaProducer<String, DocumentBatch> kafkaProducer;

    private DocumentKafkaProducer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, DocumentSerializer.class);
        props.put(CLIENT_ID_CONFIG, "document");
        kafkaProducer = new KafkaProducer<>(props);
    }

    public static DocumentKafkaProducer getInstance() {
        if (instance == null) {
            instance = new DocumentKafkaProducer();
        }
        return instance;
    }

    public boolean send(String topic, String key, DocumentBatch message) {
        DocumentCallback documentCallback = new DocumentCallback();
        ProducerRecord<String, DocumentBatch> producerRecord = new ProducerRecord<>(topic, key, message);
        Future<RecordMetadata> response = kafkaProducer.send(producerRecord, documentCallback);
        try {
            response.get();
        } catch (ExecutionException | InterruptedException e) {
            log.error("Exception occurred while getting response: {}", e.getMessage(), e);
            return false;
        }
        return documentCallback.success;
    }

    static class DocumentCallback implements Callback {

        private boolean success = true;

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e != null) {
                log.error("Unable to publish message to kafka; {}", e.getMessage(), e);
                success = false;
            } else {
                log.debug("The offset of the record we sent is: {}", recordMetadata.offset());
            }
        }
    }
}
