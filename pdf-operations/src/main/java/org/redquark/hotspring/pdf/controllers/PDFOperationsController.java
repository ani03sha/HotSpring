package org.redquark.hotspring.pdf.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redquark.hotspring.pdf.domains.stamp.StampStyle;
import org.redquark.hotspring.pdf.services.PDFOperationsService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;


@RestController
@RequestMapping("/api/v1/pdf")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "PDF Operations Controller", description = "Various operations on PDFs")
public class PDFOperationsController {

    private final PDFOperationsService pdfOperationsService;

    @PostMapping("/merge")
    @Operation(
            summary = "Merge a list of documents",
            tags = {"PDF Operations Controller"},
            responses = {
                    @ApiResponse(
                            description = "Success",
                            responseCode = "200"
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500"
                    )
            }
    )
    public ResponseEntity<InputStreamResource> mergePDF(@RequestParam("mergePDF") MultipartFile[] pdfs) {
        log.info("Request is received to merge {} PDF files", pdfs.length);
        try {
            List<InputStream> pdfList = new ArrayList<>();
            for (MultipartFile pdf : pdfs) {
                pdfList.add(pdf.getInputStream());
            }
            InputStream mergedPDFs = pdfOperationsService.merge(pdfList);
            return new ResponseEntity<>(new InputStreamResource(mergedPDFs), null, OK);
        } catch (IOException e) {
            log.error("Request for merging PDFs is failed due to: {}", e.getMessage());
            return ResponseEntity
                    .status(INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping(
            value = "/stamp",
            consumes = {
                    APPLICATION_JSON_VALUE,
                    MULTIPART_FORM_DATA_VALUE
            },
            produces = APPLICATION_PDF_VALUE)
    @Operation(
            summary = "Merge a list of documents",
            tags = {"PDF Operations Controller"},
            responses = {
                    @ApiResponse(
                            description = "Success",
                            responseCode = "200"
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500"
                    )
            }
    )
    public ResponseEntity<InputStreamResource> stampPDF(
            @RequestPart("stampPDF") MultipartFile pdf, @RequestPart("stampStyle") String stampStyleString
    ) {
        log.info("Request is received to stamp PDF: {}", pdf.getOriginalFilename());
        try {
            ObjectMapper mapper = new ObjectMapper();
            StampStyle stampStyle = mapper.readValue(stampStyleString, StampStyle.class);
            InputStream mergedPDFs = pdfOperationsService.stamp(pdf.getInputStream(), stampStyle);
            return new ResponseEntity<>(new InputStreamResource(mergedPDFs), null, OK);
        } catch (IOException e) {
            log.error("Request for merging PDFs is failed due to: {}", e.getMessage());
            return ResponseEntity
                    .status(INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
