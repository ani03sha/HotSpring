package org.redquark.hotspring.document.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redquark.hotspring.document.domains.S3FileSpecification;
import org.redquark.hotspring.document.domains.response.DocumentDownloadResponse;
import org.redquark.hotspring.document.services.DocumentDownloadService;
import org.redquark.hotspring.document.services.ProcessDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/document")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Document Controller", description = "Downloads documents from S3 bucket")
public class DocumentDownloadController {

    private final DocumentDownloadService documentDownloadService;
    private final ProcessDocumentService processDocumentService;

    @PostMapping("/download")
    @Operation(
            summary = "Download a document",
            tags = {"Document Controller"},
            responses = {
                    @ApiResponse(
                            description = "Success",
                            responseCode = "200",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = DocumentDownloadResponse.class
                                    )
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = DocumentDownloadResponse.class
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<DocumentDownloadResponse> downloadSingleFile(@RequestBody S3FileSpecification s3FileSpecification) {
        String bucket = s3FileSpecification.getBucket();
        String key = s3FileSpecification.getKey();
        try {
            log.info("Received request for downloading file={} from the S3 bucket={}", key, bucket);
            InputStream downloadedStream = documentDownloadService.downloadSingleFile(bucket, key);
            processDocumentService.processDocument(key, downloadedStream);
            log.info("Downloaded file={} from bucket={} successfully.", key, bucket);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new DocumentDownloadResponse(key, "Documents processed successfully"));
        } catch (Exception e) {
            log.error("Could not process file due to: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DocumentDownloadResponse(key, "Could not process document due to: " + e.getMessage()));
        }
    }

    @PostMapping("/download/all")
    @Operation(
            summary = "Download all documents in a folder",
            tags = {"Document Controller"},
            responses = {
                    @ApiResponse(
                            description = "Success",
                            responseCode = "200",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = DocumentDownloadResponse.class
                                    )
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = DocumentDownloadResponse.class
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<DocumentDownloadResponse> downloadAllFiles(@RequestBody S3FileSpecification s3FileSpecification) {
        String bucket = s3FileSpecification.getBucket();
        String folder = s3FileSpecification.getKey();
        log.info("Request received for downloading all files in the directory={} from bucket={}", folder, bucket);
        try {
            documentDownloadService.downloadAllFiles(bucket, folder);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new DocumentDownloadResponse(folder, "Downloaded all files successfully."));
        } catch (Exception e) {
            log.error("Could not download all files in the directory={}", folder);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DocumentDownloadResponse(folder, "Could not download all files due to: " + e.getMessage()));
        }
    }
}
