package org.redquark.hotspring.fileprocessor.services.archiver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class ZipService {

    private static final int BUFFER_SIZE = 1 << 12;
    // List to store all the files in the directory to be zipped
    private final List<String> allFilesToBeZipped = new ArrayList<>();

    public byte[] zip(String destinationDirectory, File directory) {
        log.info("Zipping files in the directory={}", destinationDirectory);
        ByteArrayOutputStream zipByteStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(zipByteStream)) {
            populateAllFilesInDirectory(directory);
            for (String filePath : allFilesToBeZipped) {
                log.info("Adding file={} to the zip", filePath);
                ZipEntry zipEntry = new ZipEntry(filePath.substring(directory.getAbsolutePath().length() + 1));
                zipOutputStream.putNextEntry(zipEntry);
                FileInputStream fileInputStream = new FileInputStream(filePath);
                byte[] buffer = new byte[BUFFER_SIZE];
                int size;
                while ((size = fileInputStream.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, size);
                }
                zipOutputStream.closeEntry();
                fileInputStream.close();
            }
        } catch (IOException e) {
            log.info("Exception occurred while zipping the files: {}", e.getMessage(), e);
        }
        return zipByteStream.toByteArray();
    }

    public void unzip(InputStream zippedIs, String destinationPath) {
        File destinationDirectory = new File(destinationPath);
        if (!destinationDirectory.exists()) {
            boolean isCreated = destinationDirectory.mkdir();
            log.info("Destination directory is created: {}", isCreated ? destinationDirectory.getName() : "");
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(zippedIs)) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                String filePath = destinationPath + File.separator + zipEntry.getName();
                if (!zipEntry.isDirectory()) {
                    extractFile(zipInputStream, filePath);
                } else {
                    File directory = new File(filePath);
                    boolean isDirectoryCreated = directory.mkdirs();
                    log.info("A new directory is created: {}", isDirectoryCreated ? directory.getName() : "");
                }
                zipInputStream.closeEntry();
                zipEntry = zipInputStream.getNextEntry();
            }
        } catch (IOException e) {
            log.error("Exception occurred while extracting zip file: {}", e.getMessage(), e);
        }
    }

    private void extractFile(ZipInputStream zipInputStream, String filePath) {
        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytes = new byte[BUFFER_SIZE];
            int read;
            while ((read = zipInputStream.read(bytes)) != -1) {
                bufferedOutputStream.write(bytes, 0, read);
            }
        } catch (IOException e) {
            log.error("Exception occurred while extracting zip file: {}", e.getMessage(), e);
        }
    }

    private void populateAllFilesInDirectory(File directory) {
        // Get all the files in the given directory, including the files in nested directories
        File[] files = directory.listFiles();
        for (File file : Objects.requireNonNull(files)) {
            if (file.isFile()) {
                allFilesToBeZipped.add(file.getAbsolutePath());
            } else {
                populateAllFilesInDirectory(file);
            }
        }
    }
}
