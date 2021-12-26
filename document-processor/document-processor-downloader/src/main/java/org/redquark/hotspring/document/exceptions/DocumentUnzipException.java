package org.redquark.hotspring.document.exceptions;

public class DocumentUnzipException extends RuntimeException {

    public DocumentUnzipException(String message, Exception e) {
        super(message, e);
    }
}
