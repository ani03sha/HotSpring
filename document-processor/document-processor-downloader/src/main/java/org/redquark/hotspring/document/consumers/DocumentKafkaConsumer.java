package org.redquark.hotspring.document.consumers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.redquark.hotspring.document.configs.KafkaDownloaderConfig;
import org.redquark.hotspring.document.deserializers.DocumentDeserializer;
import org.redquark.hotspring.document.domains.Document;
import org.redquark.hotspring.document.domains.DocumentBatch;
import org.redquark.hotspring.document.services.S3StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentKafkaConsumer implements CommandLineRunner {

    private final KafkaDownloaderConfig kafkaDownloaderConfig;
    private final S3StorageService s3StorageService;

    public static KafkaConsumer<String, DocumentBatch> getKafkaConsumer() {
        return new KafkaConsumer<>(getKafkaConsumerConfig());
    }

    private static Properties getKafkaConsumerConfig() {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.CLIENT_ID_CONFIG, "document");
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, DocumentDeserializer.class);
        consumerProps.put("value.class.name", DocumentBatch.class);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "document");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return consumerProps;
    }

    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    public void run(String... args) {
        KafkaConsumer<String, DocumentBatch> kafkaConsumer = getKafkaConsumer();
        kafkaConsumer.subscribe(Collections.singletonList(kafkaDownloaderConfig.getTopic()));
        while (true) {
            try {
                ConsumerRecords<String, DocumentBatch> consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(1));
                consumerRecords.forEach(consumerRecord -> {
                    List<Document> documents = consumerRecord.value().getDocuments();
                    if (!documents.isEmpty()) {
                        log.info("{} files received in the message", documents.size());
                        s3StorageService.uploadMultipleFiles(documents);
                    }
                });
            } catch (NullPointerException e) {
                log.info("Exception occurred while reading file: {}", e.getMessage(), e);
            }
        }
    }
}
