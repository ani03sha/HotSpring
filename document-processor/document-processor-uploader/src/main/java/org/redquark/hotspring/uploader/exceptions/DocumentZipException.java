package org.redquark.hotspring.uploader.exceptions;

public class DocumentZipException extends RuntimeException {

    public DocumentZipException(String message, Exception e) {
        super(message, e);
    }
}
