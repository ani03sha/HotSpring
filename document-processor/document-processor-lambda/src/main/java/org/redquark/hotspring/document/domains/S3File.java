package org.redquark.hotspring.document.domains;

import lombok.Data;

@Data
public class S3File {

    private String bucket;
    private String key;
}
