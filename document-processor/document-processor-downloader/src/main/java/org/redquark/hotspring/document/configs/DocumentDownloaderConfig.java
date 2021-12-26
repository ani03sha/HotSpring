package org.redquark.hotspring.document.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentDownloaderConfig {

    @Bean
    @ConfigurationProperties(prefix = "s3-downloader-config")
    public S3DownloaderConfig getS3DownloaderConfig() {
        return new S3DownloaderConfig();
    }

    @Bean
    @ConfigurationProperties(prefix = "crypto-downloader-config")
    public CryptoDownloaderConfig getCryptoDownloaderConfig() {
        return new CryptoDownloaderConfig();
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka-downloader-config")
    public KafkaDownloaderConfig getKafkaDownloaderConfig() {
        return new KafkaDownloaderConfig();
    }
}
