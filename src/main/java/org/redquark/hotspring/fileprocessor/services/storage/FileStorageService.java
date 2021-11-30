package org.redquark.hotspring.fileprocessor.services.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileStorageService implements StorageService {

    private final Path root = Paths.get("uploads");

    @Override
    public void init() {
        try {
            log.info("Creating directory for storing files");
            Files.createDirectory(root);
        } catch (IOException e) {
            log.error("Could not create root folder for file uploads: {}", e.getMessage(), e);
        }
    }

    @Override
    public void upload(MultipartFile file) {
        try {
            log.info("Saving file={} in the root folder", file.getOriginalFilename());
            Files.copy(file.getInputStream(), this.root.resolve(Objects.requireNonNull(file.getOriginalFilename())));
        } catch (IOException e) {
            log.error("Could not save file={} due to exception: {}", file.getOriginalFilename(), e.getMessage(), e);
        }
    }

    @Override
    public Resource load(String fileName) {
        log.info("Loading file={}...", fileName);
        try {
            Path filePath = root.resolve(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
        } catch (MalformedURLException e) {
            log.error("Could not read the file: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void deleteAll() {
        log.info("Deleting all files from the root");
        FileSystemUtils.deleteRecursively(root.toFile());
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.root, 1)
                    .filter(path -> !path.equals(this.root))
                    .map(this.root::relativize);
        } catch (IOException e) {
            log.error("Could not load all files in the root due to {}", e.getMessage(), e);
        }
        return null;
    }
}
