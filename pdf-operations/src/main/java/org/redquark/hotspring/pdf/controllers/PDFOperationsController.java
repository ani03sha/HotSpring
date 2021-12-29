package org.redquark.hotspring.pdf.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redquark.hotspring.pdf.domains.response.PDFMergeResponse;
import org.redquark.hotspring.pdf.services.PDFOperationsService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


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
                            responseCode = "200",
                            content = @Content(
                                    mediaType = "application/pdf",
                                    schema = @Schema(
                                            implementation = InputStreamResource.class
                                    )
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = PDFMergeResponse.class
                                    )
                            )
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
            return new ResponseEntity<>(new InputStreamResource(mergedPDFs), null, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Request for merging PDFs is failed due to: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
