package org.redquark.hotspring.fileprocessor.controllers;

import lombok.extern.slf4j.Slf4j;
import org.redquark.hotspring.fileprocessor.domains.FileInfo;
import org.redquark.hotspring.fileprocessor.domains.response.FileUploadResponse;
import org.redquark.hotspring.fileprocessor.services.process.ProcessFiles;
import org.redquark.hotspring.fileprocessor.services.storage.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping(value = "/api/v1/files")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final ProcessFiles processFiles;

    public FileUploadController(FileStorageService fileStorageService, ProcessFiles processFiles) {
        this.fileStorageService = fileStorageService;
        this.processFiles = processFiles;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile[] files) {
        String message;
        try {
            Arrays.stream(files).forEach(fileStorageService::upload);
            message = "Uploaded all files successfully!";
            log.info(message);
            log.info("File processing starts...");
            processFiles.process();
            log.info("File processing ends...");
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new FileUploadResponse(message));
        } catch (Exception e) {
            message = "Could not upload the files";
            log.error("{} due to exception: {}", message, e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .body(new FileUploadResponse(message));
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<FileInfo>> getFiles() {
        List<FileInfo> fileInfos = fileStorageService.loadAll().map(path -> {
            String fileName = path.getFileName().toString();
            String url = MvcUriComponentsBuilder.fromMethodName(FileUploadController.class, "getFile", path.getFileName().toString()).build().toString();
            return new FileInfo(fileName, url);
        }).collect(Collectors.toList());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(fileInfos);
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource file = fileStorageService.load(filename);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @GetMapping("/clear")
    public ResponseEntity<FileUploadResponse> clearAllFiles() {
        String message;
        try {
            processFiles.clearAllFiles();
            message = "Cleared all files";
            return ResponseEntity.status(HttpStatus.OK).body(new FileUploadResponse(message));
        } catch (Exception e) {
            message = "Could not clear files";
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new FileUploadResponse(message));
        }
    }
}
