package org.redquark.hotspring.fileprocessor.services.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.redquark.hotspring.fileprocessor.services.s3.connection.AmazonS3ConnectionFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
@Slf4j
public class S3StorageService {

    private final AmazonS3 amazonS3;

    public S3StorageService(AmazonS3ConnectionFactory amazonS3ConnectionFactory) {
        this.amazonS3 = amazonS3ConnectionFactory.getConnection();
    }

    public PutObjectResult upload(String bucket,
                                  String key,
                                  Map<String, String> optionalMetadata,
                                  InputStream fileInputStream) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        if (optionalMetadata != null && !optionalMetadata.isEmpty()) {
            optionalMetadata.forEach(objectMetadata::addUserMetadata);
        }
        try {
            log.info("Storing {} in bucket={}", key, bucket);
            return amazonS3.putObject(bucket, key, fileInputStream, objectMetadata);
        } catch (AmazonServiceException e) {
            log.error("Exception occurred while store the file in the S3 bucket: {}", e.getMessage(), e);
        }
        return null;
    }

    public byte[] retrieve(String bucket, String key) {
        try {
            S3Object s3Object = amazonS3.getObject(bucket, key);
            S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();
            return IOUtils.toByteArray(s3ObjectInputStream);
        } catch (IOException e) {
            log.error("Exception occurred while retrieving object from the S3 bucket: {}", e.getMessage(), e);
        }
        return null;
    }
}
