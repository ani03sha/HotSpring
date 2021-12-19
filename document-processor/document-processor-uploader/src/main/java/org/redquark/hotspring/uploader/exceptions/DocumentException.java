package org.redquark.hotspring.uploader.exceptions;

public class DocumentException extends RuntimeException {

    public DocumentException(String message, Exception e) {
        super(message, e);
    }
}
