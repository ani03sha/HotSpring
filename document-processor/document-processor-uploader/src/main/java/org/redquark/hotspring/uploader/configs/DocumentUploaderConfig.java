package org.redquark.hotspring.uploader.configs;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentUploaderConfig {

    @Bean
    @ConfigurationProperties(prefix = "s3-uploader-config")
    public S3Config getS3Config() {
        return new S3Config();
    }

    @Bean
    @ConfigurationProperties(prefix = "crypto-uploader-config")
    public CryptoConfig getCryptoConfig() {
        return new CryptoConfig();
    }
}
