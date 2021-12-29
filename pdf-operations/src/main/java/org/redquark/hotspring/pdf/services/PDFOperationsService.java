package org.redquark.hotspring.pdf.services;

import java.io.InputStream;
import java.util.List;

public interface PDFOperationsService {

    InputStream merge(List<InputStream> pdfList);
}
