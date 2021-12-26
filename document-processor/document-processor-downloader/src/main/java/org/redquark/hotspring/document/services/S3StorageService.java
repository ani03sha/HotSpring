package org.redquark.hotspring.document.services;

import org.redquark.hotspring.document.domains.Document;

import java.util.List;

public interface S3StorageService {

    void uploadMultipleFiles(List<Document> documents);
}
