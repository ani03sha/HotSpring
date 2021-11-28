package org.redquark.hotspring.fileprocessor.services.s3.connection;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.redquark.hotspring.fileprocessor.config.AWSConfig;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class AmazonS3ConnectionFactory {

    private final String accessKey;
    private final String secretKey;
    private final String region;
    private AmazonS3 amazonS3;

    public AmazonS3ConnectionFactory(AWSConfig awsConfig) {
        this.accessKey = awsConfig.getAccessKey();
        this.secretKey = awsConfig.getSecretKey();
        this.region = awsConfig.getRegion();
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
