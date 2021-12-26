package org.redquark.hotspring.document.configs;

import lombok.Data;

@Data
public class KafkaDownloaderConfig {

    private String topic;
    private Integer batchSize;
    private Integer retryCount;
}
