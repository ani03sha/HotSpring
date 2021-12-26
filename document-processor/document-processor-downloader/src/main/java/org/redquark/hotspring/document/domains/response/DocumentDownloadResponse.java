package org.redquark.hotspring.document.domains.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentDownloadResponse {

    private String name;
    private String message;
}
