package org.redquark.hotspring.document.domains;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class Document implements Serializable {

    private String name;
    private byte[] contents;
}
