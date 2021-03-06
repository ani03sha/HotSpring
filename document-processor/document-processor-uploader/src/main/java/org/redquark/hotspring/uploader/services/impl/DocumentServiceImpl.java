package org.redquark.hotspring.uploader.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.redquark.hotspring.uploader.domains.Document;
import org.redquark.hotspring.uploader.exceptions.DocumentException;
import org.redquark.hotspring.uploader.process.DocumentZipper;
import org.redquark.hotspring.uploader.process.S3Helper;
import org.redquark.hotspring.uploader.services.DocumentService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final String ZIP_FILE_NAME = "archived.zip";
    private final DocumentZipper zipper;
    private final S3Helper s3Helper;

    @Override
    public void upload(MultipartFile[] documents) {
        List<Document> documentList = new ArrayList<>();
        List<File> documentsToUpload = new ArrayList<>();
        try {
            for (MultipartFile document : documents) {
                File currentFile = new File(Objects.requireNonNull(document.getOriginalFilename()));
                FileUtils.writeByteArrayToFile(currentFile, document.getBytes());
                documentList.add(Document.builder().name(document.getOriginalFilename()).contents(document.getBytes()).build());
                documentsToUpload.add(currentFile);
            }
            s3Helper.uploadMultipleFiles(documentsToUpload);
            log.info("Zipping of {} files starts...", documentList.size());
            byte[] zippedBytes = zipper.zip(documentList);
            log.info("Zipping of files is completed.");
            Document zippedDocument = Document.builder().name(ZIP_FILE_NAME).contents(zippedBytes).build();
            s3Helper.upload(ZIP_FILE_NAME, new HashMap<>(), new ByteArrayInputStream(zippedDocument.getContents()));
            for (File document : documentsToUpload) {
                Files.delete(document.toPath());
            }
        } catch (IOException e) {
            throw new DocumentException("Could not extract contents of file", e);
        }
    }

    @Override
    public void delete() {
        s3Helper.delete();
    }
}
