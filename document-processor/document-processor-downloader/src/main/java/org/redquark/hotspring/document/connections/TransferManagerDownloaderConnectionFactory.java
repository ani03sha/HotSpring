package org.redquark.hotspring.document.connections;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redquark.hotspring.document.configs.S3DownloaderConfig;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static com.amazonaws.services.s3.internal.Constants.MB;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransferManagerDownloaderConnectionFactory {

    private final S3DownloaderConnectionFactory s3ConnectionFactory;
    private final S3DownloaderConfig s3Config;

    private TransferManager transferManager;


    @PostConstruct
    protected void init() {
        transferManager = TransferManagerBuilder.standard()
                .withS3Client(s3ConnectionFactory.getConnection())
                .withDisableParallelDownloads(false)
                .withMinimumUploadPartSize((long) (5 * MB))
                .withMultipartUploadThreshold((long) (16 * MB))
                .withMultipartCopyPartSize((long) (5 * MB))
                .withMultipartCopyThreshold((long) (100 * MB))
                .withExecutorFactory(this::createExecutorService)
                .build();

        int oneDay = 1000 * 60 * 60 * 24;
        Date oneDayAgo = new Date(System.currentTimeMillis() - oneDay);
        try {
            transferManager.abortMultipartUploads(s3Config.getBucketName(), oneDayAgo);
        } catch (AmazonClientException e) {
            log.error("Unable to upload file, upload was aborted, reason: {}", e.getMessage());
        }
    }

    private ThreadPoolExecutor createExecutorService() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private int threadCount = 1;

            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("jsa-amazon-s3-transfer-manager-worker-" + threadCount++);
                return thread;
            }
        };
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(20, threadFactory);
    }

    public TransferManager getTransferManager() {
        return transferManager;
    }
}
