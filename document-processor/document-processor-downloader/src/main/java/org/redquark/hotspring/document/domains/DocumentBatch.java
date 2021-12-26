package org.redquark.hotspring.document.domains;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class DocumentBatch implements Serializable {

    private List<Document> documents;
}
