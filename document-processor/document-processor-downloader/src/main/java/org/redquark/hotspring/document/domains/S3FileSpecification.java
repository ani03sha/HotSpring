package org.redquark.hotspring.document.domains;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class S3FileSpecification {

    private String bucket;
    private String key;
}
