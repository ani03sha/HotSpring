package org.redquark.hotspring.document.services.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.TransferManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.redquark.hotspring.document.configs.S3DownloaderConfig;
import org.redquark.hotspring.document.connections.S3DownloaderConnectionFactory;
import org.redquark.hotspring.document.connections.TransferManagerDownloaderConnectionFactory;
import org.redquark.hotspring.document.exceptions.DocumentDownloadException;
import org.redquark.hotspring.document.services.DocumentDownloadService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentDownloadServiceImpl implements DocumentDownloadService {

    private final S3DownloaderConnectionFactory connectionFactory;
    private final TransferManagerDownloaderConnectionFactory transferManagerConnectionFactory;
    private final S3DownloaderConfig s3DownloaderConfig;

    @Override
    public InputStream downloadSingleFile(String bucket, String key) {
        log.info("Downloading of file={} in bucket={} starts...", key, bucket);
        AmazonS3 amazonS3 = connectionFactory.getConnection();
        S3Object s3Object = amazonS3.getObject(bucket, s3DownloaderConfig.getSourceFolder() + "/" + key);
        return s3Object.getObjectContent();
    }

    @Override
    public List<InputStream> downloadAllFiles(String bucket, String key) {
        log.info("Request received for downloading all files in the folder={} of bucket={}", key, bucket);
        File destinationDirectory = new File(s3DownloaderConfig.getDestinationFolder());
        try {
            TransferManager transferManager = transferManagerConnectionFactory.getTransferManager();
            MultipleFileDownload multipleFileDownload = transferManager.downloadDirectory(bucket, s3DownloaderConfig.getSourceFolder(), destinationDirectory);
            multipleFileDownload.waitForCompletion();
            log.info("Download of all files in the folder={} is completed.", s3DownloaderConfig.getSourceFolder());
            File downloadDirectory = new File(s3DownloaderConfig.getDestinationFolder() + "/" + key);
            File[] allFiles = downloadDirectory.listFiles();
            log.info("Downloaded {} files", Objects.requireNonNull(allFiles).length);
            List<InputStream> allFilesStream = new ArrayList<>();
            for (File file : allFiles) {
                allFilesStream.add(FileUtils.openInputStream(file));
            }
            return allFilesStream;
        } catch (InterruptedException | IOException e) {
            log.error("Could not download files due to: {}", e.getMessage(), e);
            throw new DocumentDownloadException("Could not download files", e);
        }
    }
}
