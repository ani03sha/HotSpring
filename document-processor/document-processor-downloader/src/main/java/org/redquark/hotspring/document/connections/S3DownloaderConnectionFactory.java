package org.redquark.hotspring.document.connections;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.redquark.hotspring.document.configs.S3DownloaderConfig;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class S3DownloaderConnectionFactory {

    private final String accessKey;
    private final String secretKey;
    private final String region;
    private AmazonS3 amazonS3;

    public S3DownloaderConnectionFactory(S3DownloaderConfig s3DownloaderConfig) {
        this.accessKey = s3DownloaderConfig.getAccessKey();
        this.secretKey = s3DownloaderConfig.getSecretKey();
        this.region = s3DownloaderConfig.getRegion();
    }

    @PostConstruct
    protected void init() {
        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        amazonS3 = AmazonS3ClientBuilder
                .standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }

    public AmazonS3 getConnection() {
        return amazonS3;
    }
}
