package org.redquark.hotspring.document.services.impl;

import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.redquark.hotspring.document.configs.S3DownloaderConfig;
import org.redquark.hotspring.document.connections.TransferManagerDownloaderConnectionFactory;
import org.redquark.hotspring.document.domains.Document;
import org.redquark.hotspring.document.exceptions.S3StorageException;
import org.redquark.hotspring.document.services.S3StorageService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3StorageServiceImpl implements S3StorageService {

    private final TransferManagerDownloaderConnectionFactory transferManagerConnectionFactory;
    private final S3DownloaderConfig s3Config;

    @Override
    public void uploadMultipleFiles(List<Document> documents) {
        log.info("Uploading {} files to the S3 bucket", documents.size());
        try {
            List<File> filesToUpload = new ArrayList<>();
            for (Document document : documents) {
                File file = new File(document.getName());
                FileUtils.writeByteArrayToFile(file, document.getContents());
                filesToUpload.add(file);
            }
            TransferManager transferManager = transferManagerConnectionFactory.getTransferManager();
            MultipleFileUpload upload = transferManager.uploadFileList(s3Config.getBucketName(), s3Config.getUploadFolder(), new File("."), filesToUpload);
            upload.waitForCompletion();
            for (File file : filesToUpload) {
                Files.delete(file.toPath());
            }
        } catch (InterruptedException | IOException e) {
            log.info("Could not upload {} files to the bucket: {}", documents.size(), s3Config.getBucketName());
            throw new S3StorageException("Could not upload files to the S3 bucket", e);
        }
    }
}
