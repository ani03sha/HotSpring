package org.redquark.hotspring.document.configs;

import lombok.Data;

@Data
public class S3DownloaderConfig {

    private String endpointUrl;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String region;
    private String sourceFolder;
    private String destinationFolder;
    private String uploadFolder;
}
