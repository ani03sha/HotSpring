package org.redquark.hotspring.document.exceptions;

public class DecryptionException extends RuntimeException {

    public DecryptionException(String message, Exception e) {
        super(message, e);
    }
}
