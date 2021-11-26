package org.redquark.hotspring.fileprocessor.config;

import lombok.Data;

@Data
public class AWSConfig {

    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
    private String s3Retrieved;
}
