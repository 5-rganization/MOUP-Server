package com.moup.global.infra.file;

import com.moup.global.error.InvalidFileExtensionException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {
    public void verifyFileExtension(MultipartFile file, File extension) {
        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith(extension.getContentType())) {
            throw new InvalidFileExtensionException();
        }
    }
}
