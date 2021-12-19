package org.redquark.hotspring.uploader.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redquark.hotspring.uploader.domains.responses.DocumentDeleteResponse;
import org.redquark.hotspring.uploader.domains.responses.DocumentUploadResponse;
import org.redquark.hotspring.uploader.services.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/document")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Document Controller", description = "Uploads/deletes documents to/from S3 bucket")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    @Operation(
            summary = "Upload multiple documents",
            tags = {"Document Controller"},
            responses = {
                    @ApiResponse(
                            description = "Success",
                            responseCode = "200",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = DocumentUploadResponse.class
                                    )
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = DocumentUploadResponse.class
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<DocumentUploadResponse> uploadDocuments(@RequestParam("documents") MultipartFile[] documents) {
        String message;
        try {
            log.info("Uploading {} files in S3", documents.length);
            documentService.upload(documents);
            message = "Files uploaded successfully";
            log.info(message);
            return ResponseEntity.
                    status(HttpStatus.OK)
                    .body(new DocumentUploadResponse(message));
        } catch (Exception e) {
            message = "Could not upload files";
            log.error("{} due to exception: {}", message, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .body(new DocumentUploadResponse(message));
        }
    }

    @PostMapping("/delete")
    @Operation(
            summary = "Delete document by given key",
            tags = {"Document Controller"},
            responses = {
                    @ApiResponse(
                            description = "Success",
                            responseCode = "200",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = DocumentDeleteResponse.class
                                    )
                            )
                    ),
                    @ApiResponse(
                            description = "Internal server error",
                            responseCode = "500",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = DocumentDeleteResponse.class
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<DocumentDeleteResponse> deleteFile() {
        String message;
        try {
            log.info("Clearing bucket...");
            documentService.delete();
            message = "File deleted successfully";
            log.info(message);
            return ResponseEntity.
                    status(HttpStatus.OK)
                    .body(new DocumentDeleteResponse(message));
        } catch (Exception e) {
            message = "Could not upload files";
            log.error("{} due to: {}", message, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .body(new DocumentDeleteResponse(message));
        }
    }
}
