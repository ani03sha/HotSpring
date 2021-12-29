package org.redquark.hotspring.pdf.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.redquark.hotspring.pdf.exceptions.PDFMergingException;
import org.redquark.hotspring.pdf.services.PDFOperationsService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PDFOperationsServiceImpl implements PDFOperationsService {

    private final PDFMergerUtility pdfMergerUtility;

    @Override
    public InputStream merge(List<InputStream> pdfList) {
        log.info("Merging of {} pdf files starts...", pdfList.size());
        try {
            pdfMergerUtility.addSources(pdfList);
            ByteArrayOutputStream mergedPdf = new ByteArrayOutputStream();
            pdfMergerUtility.setDestinationStream(mergedPdf);
            pdfMergerUtility.mergeDocuments(null);
            byte[] mergedPdfBytes = mergedPdf.toByteArray();
            log.info("Merging of {} pdf files is completed successfully", pdfList.size());
            return new ByteArrayInputStream(mergedPdfBytes);
        } catch (IOException e) {
            log.error("Could not merge PDFs due to: {}", e.getMessage());
            throw new PDFMergingException(e.getMessage(), e);
        }
    }
}
