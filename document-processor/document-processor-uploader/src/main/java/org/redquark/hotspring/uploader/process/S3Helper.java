package org.redquark.hotspring.uploader.process;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import lombok.extern.slf4j.Slf4j;
import org.redquark.hotspring.uploader.configs.DocumentUploaderConfig;
import org.redquark.hotspring.uploader.connections.S3ConnectionFactory;
import org.redquark.hotspring.uploader.connections.TransferManagerConnectionFactory;
import org.redquark.hotspring.uploader.exceptions.DocumentException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class S3Helper {

    private final AmazonS3 amazonS3;
    private final TransferManager transferManager;

    private final String bucketName;
    private final String folderName;

    public S3Helper(
            DocumentUploaderConfig documentUploaderConfig,
            S3ConnectionFactory s3ConnectionFactory,
            TransferManagerConnectionFactory transferManagerConnectionFactory
    ) {
        this.amazonS3 = s3ConnectionFactory.getConnection();
        this.transferManager = transferManagerConnectionFactory.getTransferManager();
        this.bucketName = documentUploaderConfig.getS3Config().getBucketName();
        this.folderName = documentUploaderConfig.getS3Config().getUploadFolder();
    }

    public void upload(
            String key,
            Map<String, String> optionalMetadata,
            InputStream fileInputStream
    ) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        if (optionalMetadata != null && !optionalMetadata.isEmpty()) {
            optionalMetadata.forEach(objectMetadata::addUserMetadata);
        }
        try {
            log.info("Storing {} in bucket={}", key, bucketName);
            PutObjectResult result = amazonS3.putObject(bucketName, folderName + "/" + key, fileInputStream, objectMetadata);
            log.info("Object is stored with versionId={}", result.getMetadata().getVersionId());
        } catch (AmazonServiceException e) {
            log.error("Exception occurred while store the file in the S3 bucket: {}", e.getMessage(), e);
            throw new DocumentException("Could not upload file to S3", e);
        }
    }

    public void uploadMultipleFiles(List<File> documentsToUpload) {
        MultipleFileUpload multipleFileUpload = transferManager.uploadFileList(bucketName, folderName, new File("."), documentsToUpload);
        try {
            multipleFileUpload.waitForCompletion();
        } catch (InterruptedException e) {
            log.error("Exception occurred while uploading multiple files: {}", e.getMessage());
            throw new DocumentException("Could not upload multiple files", e);
        }
    }

    public void delete() {
        if (amazonS3.doesBucketExistV2(bucketName)) {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                    .withBucketName(bucketName)
                    .withPrefix(folderName);
            ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);
            log.info("Found {} objects to delete in the bucket={}", objectListing.getObjectSummaries().size(), bucketName);
            while (true) {
                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    log.info("Deleting {} from bucket={}", objectSummary.getKey(), bucketName);
                    amazonS3.deleteObject(bucketName, objectSummary.getKey());
                }
                if (objectListing.isTruncated()) {
                    objectListing = amazonS3.listNextBatchOfObjects(objectListing);
                } else {
                    break;
                }
            }
        }
    }
}
