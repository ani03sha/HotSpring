package org.redquark.hotspring.document.services.impl;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redquark.hotspring.document.configs.KafkaDownloaderConfig;
import org.redquark.hotspring.document.domains.Document;
import org.redquark.hotspring.document.domains.DocumentBatch;
import org.redquark.hotspring.document.producers.DocumentKafkaProducer;
import org.redquark.hotspring.document.services.MessagePublishingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessagePublishingServiceImpl implements MessagePublishingService {

    private final DocumentKafkaProducer producer = DocumentKafkaProducer.getInstance();

    private final KafkaDownloaderConfig kafkaConfig;

    @Override
    public void publishDocuments(String key, List<Document> documents) {
        log.info("Publishing messages for document");
        int batchSize = kafkaConfig.getBatchSize();
        List<List<Document>> documentsList = Lists.partition(documents, batchSize);
        int retryCount = kafkaConfig.getRetryCount();
        for (int i = 0; i < documentsList.size(); i++) {
            log.info("Publishing batch number: {}", i);
            DocumentBatch batch = DocumentBatch.builder().documents(new ArrayList<>(documentsList.get(i))).build();
            boolean isPublished = producer.send(kafkaConfig.getTopic(), key, batch);
            while (!isPublished && retryCount > 0) {
                log.info("Could not publish the message. {} retries left.", retryCount);
                isPublished = producer.send(kafkaConfig.getTopic(), key, batch);
                retryCount--;
            }
            if (isPublished) {
                log.info("Message published successfully");
            } else {
                log.info("Could not publish the message successfully. Terminating the process!");
            }
        }
        log.info("All the documents have been published");
    }
}
