package org.redquark.hotspring.document.exceptions;

public class S3StorageException extends RuntimeException {

    public S3StorageException(String message, Exception e) {
        super(message, e);
    }
}
