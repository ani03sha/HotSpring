package org.redquark.hotspring.document.services;

import org.redquark.hotspring.document.domains.Document;

import java.util.List;

public interface MessagePublishingService {

    void publishDocuments(String key, List<Document> documents);
}
