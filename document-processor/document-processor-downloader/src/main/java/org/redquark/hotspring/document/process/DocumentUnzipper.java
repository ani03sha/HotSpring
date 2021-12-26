package org.redquark.hotspring.document.process;

import lombok.extern.slf4j.Slf4j;
import org.redquark.hotspring.document.domains.Document;
import org.redquark.hotspring.document.exceptions.DocumentUnzipException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Slf4j
public class DocumentUnzipper {

    private static final int BUFFER_SIZE = 1 << 10;

    public List<Document> unzip(InputStream zippedIs) {
        List<Document> unzippedFiles = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(zippedIs)) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                byte[] bytes = new byte[0];
                if (!zipEntry.isDirectory()) {
                    bytes = extractFile(zipInputStream);
                }
                unzippedFiles.add(Document.builder().name(zipEntry.getName()).contents(bytes).build());
                zipInputStream.closeEntry();
                zipEntry = zipInputStream.getNextEntry();
            }
            return unzippedFiles;
        } catch (IOException e) {
            log.error("Exception occurred while extracting zip file: {}", e.getMessage(), e);
            throw new DocumentUnzipException("Could not unzip the document", e);
        }
    }

    private byte[] extractFile(ZipInputStream zipInputStream) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[BUFFER_SIZE];
            int read;
            while ((read = zipInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Exception occurred while extracting zip file: {}", e.getMessage(), e);
        }
        return null;
    }
}
