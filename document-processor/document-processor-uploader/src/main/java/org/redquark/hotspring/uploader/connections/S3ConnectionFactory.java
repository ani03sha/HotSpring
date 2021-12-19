package org.redquark.hotspring.uploader.connections;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.redquark.hotspring.uploader.configs.S3Config;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class S3ConnectionFactory {

    private final String accessKey;
    private final String secretKey;
    private final String region;
    private AmazonS3 amazonS3;

    public S3ConnectionFactory(S3Config s3Config) {
        this.accessKey = s3Config.getAccessKey();
        this.secretKey = s3Config.getSecretKey();
        this.region = s3Config.getRegion();
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

