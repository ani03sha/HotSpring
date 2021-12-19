package org.redquark.hotspring.uploader.configs;

import lombok.Data;

@Data
public class S3Config {

    private String endpointUrl;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String region;
    private String uploadFolder;
}
