package org.redquark.hotspring.document.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redquark.hotspring.document.domains.Document;
import org.redquark.hotspring.document.process.DocumentUnzipper;
import org.redquark.hotspring.document.services.MessagePublishingService;
import org.redquark.hotspring.document.services.ProcessDocumentService;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessDocumentServiceImpl implements ProcessDocumentService {

    private final DocumentUnzipper unzipper;
    private final MessagePublishingService messagePublishingService;

    @Override
    public void processDocument(String name, InputStream data) {
        log.info("Processing of file={} starts...", name);
        log.info("Unzipping of file={} starts...", name);
        List<Document> unzippedFiles = unzipper.unzip(data);
        log.info("Unzipping of file={} ends. Found {} files in the zip", name, unzippedFiles.size());
        log.info("Publishing of documents starts...");
        messagePublishingService.publishDocuments(name, unzippedFiles);
    }
}
