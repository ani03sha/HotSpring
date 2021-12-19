package org.redquark.hotspring.uploader.process;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.redquark.hotspring.uploader.domains.Document;
import org.redquark.hotspring.uploader.exceptions.DocumentException;
import org.redquark.hotspring.uploader.exceptions.DocumentZipException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@Slf4j
public class DocumentZipper {

    private static final String ZIP_FILE_NAME = "archived.zip";
    private static final int BUFFER_SIZE = 1 << 10;

    public byte[] zip(List<Document> documents) {
        log.info("Zipping {} files", documents.size());
        final File targetZip = new File(ZIP_FILE_NAME);
        try (
                OutputStream outputStream = new FileOutputStream(targetZip);
                ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)
        ) {
            byte[] buffer = new byte[BUFFER_SIZE];
            for (Document document : documents) {
                File currentFile = getCurrentFile(document);
                if (!currentFile.isDirectory()) {
                    ZipEntry zipEntry = new ZipEntry(currentFile.getName());
                    InputStream fileInputStream = new FileInputStream(currentFile);
                    zipOutputStream.putNextEntry(zipEntry);
                    int read;
                    while ((read = fileInputStream.read(buffer)) != -1) {
                        zipOutputStream.write(buffer, 0, read);
                    }
                    zipOutputStream.closeEntry();
                    fileInputStream.close();
                }
            }
            log.info("Zipping of files is completed");
            return FileUtils.readFileToByteArray(targetZip);
        } catch (IOException e) {
            log.error("Exception occurred while zipping files");
            throw new DocumentZipException("Could not zip files", e);
        }
    }

    private File getCurrentFile(Document document) {
        OutputStream currentFileOS = null;
        try {
            File currentFile = new File(document.getName());
            currentFileOS = new FileOutputStream(currentFile);
            FileUtils.writeByteArrayToFile(currentFile, document.getContents());
            return currentFile;
        } catch (IOException e) {
            log.error("Exception occurred while reading file: {}", e.getMessage());
            throw new DocumentException("Could not zip files", e);
        } finally {
            try {
                if (currentFileOS != null) {
                    currentFileOS.close();
                }
            } catch (IOException e) {
                log.error("Exception occurred while closing the stream: {}", e.getMessage(), e);
            }
        }
    }
}
