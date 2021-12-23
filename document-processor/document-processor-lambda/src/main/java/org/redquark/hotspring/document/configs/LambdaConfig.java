package org.redquark.hotspring.document.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LambdaConfig {

    @Bean
    @ConfigurationProperties(prefix = "document-processor-downloader.download-endpoint")
    public DownloaderConfig getDownloaderConfig() {
        return new DownloaderConfig();
    }
}
