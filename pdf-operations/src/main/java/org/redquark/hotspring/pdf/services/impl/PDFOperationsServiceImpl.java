package org.redquark.hotspring.pdf.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.util.Matrix;
import org.redquark.hotspring.pdf.domains.stamp.StampStyle;
import org.redquark.hotspring.pdf.exceptions.PDFMergingException;
import org.redquark.hotspring.pdf.exceptions.PDFStampException;
import org.redquark.hotspring.pdf.services.PDFOperationsService;
import org.springframework.stereotype.Service;

import java.awt.Color;
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

    @Override
    public InputStream stamp(InputStream pdf, StampStyle stampStyle) {
        log.info("Stamping of pdf document starts...");
        if (stampStyle == null) {
            log.error("Could not proceed with stamping because StampStyle is null");
            throw new PDFStampException("StampStyle is null. Cannot proceed.", null);
        }
        if (stampStyle.getText() == null || stampStyle.getText().isEmpty()) {
            log.error("Could not proceed with stamping because stamp text is empty or null");
            throw new PDFStampException("Stamp text is null or empty. Cannot proceed.", null);
        }
        String stampText = stampStyle.getText();
        double fontHeight = stampStyle.getFontHeight();
        double rotation = Math.toRadians(stampStyle.getRotation());
        String color = stampStyle.getColor();
        double opacity = stampStyle.getOpacity();
        double horizontalOffset = stampStyle.getHorizontalOffset();
        double verticalOffset = stampStyle.getVerticalOffset();
        try (PDDocument pdDocument = PDDocument.load(pdf)) {
            PDFont font = PDType1Font.HELVETICA;
            ByteArrayOutputStream stamped = new ByteArrayOutputStream();
            for (PDPage page : pdDocument.getPages()) {
                try (PDPageContentStream contentStream = new PDPageContentStream(
                        pdDocument,
                        page,
                        PDPageContentStream.AppendMode.APPEND,
                        true,
                        true
                )) {
                    // Normalized width
                    double stringWidth = font.getStringWidth(stampText) / 1000 * fontHeight;
                    // Rotational components
                    double horizontal = Math.cos(rotation) * stringWidth;
                    double vertical = Math.sin(rotation) * stringWidth;
                    // Width and height of the page
                    double width = page.getCropBox().getWidth();
                    double height = page.getCropBox().getHeight();
                    // X and Y coordinates of the stamp
                    double tx = (width - horizontal) / 2 + horizontalOffset * width / 200;
                    double ty = (height - vertical) / 2 + verticalOffset * height / 200;
                    // Rotate the stamp text and shift it to the calculated x,y coordinates
                    // with respect to the centre of the page
                    contentStream.transform(Matrix.getTranslateInstance((float) tx, (float) ty));
                    contentStream.transform(Matrix.getRotateInstance(rotation, 0, 0));
                    contentStream.setFont(font, (float) fontHeight);
                    contentStream.setRenderingMode(RenderingMode.FILL);
                    contentStream.setNonStrokingColor(Color.decode(color.trim()));
                    // Styling for the watermark
                    PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                    graphicsState.setNonStrokingAlphaConstant((float) opacity);
                    graphicsState.setStrokingAlphaConstant((float) opacity);
                    graphicsState.getCOSObject().setItem(COSName.BM, COSName.MULTIPLY);
                    contentStream.setGraphicsStateParameters(graphicsState);
                    // Writing into file
                    contentStream.beginText();
                    contentStream.showText(stampText);
                    contentStream.endText();
                }
                pdDocument.save(stamped);
            }
            log.info("Stamping is completed successfully");
            return new ByteArrayInputStream(stamped.toByteArray());
        } catch (IOException e) {
            log.error("Could not stamp pdf due to: {}", e.getMessage());
            throw new PDFStampException(e.getMessage(), e);
        }
    }
}
