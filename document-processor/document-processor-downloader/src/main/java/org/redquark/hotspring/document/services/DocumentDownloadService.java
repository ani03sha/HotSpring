package org.redquark.hotspring.document.services;

import java.io.InputStream;
import java.util.List;

public interface DocumentDownloadService {

    InputStream downloadSingleFile(String bucket, String key);

    List<InputStream> downloadAllFiles(String bucket, String key);
}
