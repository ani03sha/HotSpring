package org.redquark.hotspring.fileprocessor.domains;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnzippedFile {
    private String name;
    private byte[] bytes;
}
