package org.redquark.hotspring.document.services;

import java.io.InputStream;

public interface ProcessDocumentService {

    void processDocument(String name, InputStream data);
}
