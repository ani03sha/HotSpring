package org.redquark.hotspring.fileprocessor;

import org.redquark.hotspring.fileprocessor.services.storage.FileStorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class FileUploadInitializer implements CommandLineRunner {

    private final FileStorageService fileStorageService;

    public FileUploadInitializer(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;

    }

    @Override
    public void run(String... args) {
        fileStorageService.deleteAll();
        fileStorageService.init();
    }
}
