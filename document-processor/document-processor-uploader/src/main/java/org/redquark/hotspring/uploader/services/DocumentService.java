package org.redquark.hotspring.uploader.services;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    void upload(MultipartFile[] documents);

    void delete();
}
