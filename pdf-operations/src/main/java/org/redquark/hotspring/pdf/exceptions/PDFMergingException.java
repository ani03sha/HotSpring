package org.redquark.hotspring.pdf.exceptions;

public class PDFMergingException extends RuntimeException {

    public PDFMergingException(String message, Exception e) {
        super(message, e);
    }
}
