package org.redquark.hotspring.document.exceptions;

public class DocumentDownloadException extends RuntimeException {

    public DocumentDownloadException(String message, Exception e) {
        super(message, e);
    }
}
