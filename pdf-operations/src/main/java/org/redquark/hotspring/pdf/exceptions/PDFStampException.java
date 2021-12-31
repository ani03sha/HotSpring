package org.redquark.hotspring.pdf.exceptions;

public class PDFStampException extends RuntimeException {

    public PDFStampException(String message, Exception e) {
        super(message, e);
    }
}
