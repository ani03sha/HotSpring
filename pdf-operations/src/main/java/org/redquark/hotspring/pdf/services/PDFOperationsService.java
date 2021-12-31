package org.redquark.hotspring.pdf.services;

import org.redquark.hotspring.pdf.domains.stamp.StampStyle;

import java.io.InputStream;
import java.util.List;

public interface PDFOperationsService {

    InputStream merge(List<InputStream> pdfList);

    InputStream stamp(InputStream pdf, StampStyle stampStyle);
}
