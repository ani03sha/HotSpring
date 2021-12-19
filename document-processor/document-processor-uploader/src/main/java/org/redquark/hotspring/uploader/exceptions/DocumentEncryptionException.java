package org.redquark.hotspring.uploader.exceptions;

public class DocumentEncryptionException extends RuntimeException {

    public DocumentEncryptionException(String message, Exception e) {
        super(message, e);
    }
}
