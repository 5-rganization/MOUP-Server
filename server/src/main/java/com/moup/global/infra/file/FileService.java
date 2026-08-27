package com.moup.global.infra.file;

import com.moup.global.error.InvalidFileExtensionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class FileService {

    /// 업로드 파일이 실제 이미지인지 **내용으로** 확인하고 안전한 확장자를 돌려준다.
    ///
    /// `Content-Type`과 원본 파일명은 클라이언트가 정하는 값이라 신뢰할 수 없다.
    /// 예전에는 `Content-Type`이 `image/`로 시작하는지만 봤고 저장할 때는 원본 확장자를
    /// 그대로 이어붙여서, `.html`이나 `.js`를 `image/png`라고 주장하며 올릴 수 있었다.
    ///
    /// @return 판별된 종류의 표준 확장자 (`.jpg` / `.png` / `.gif`)
    public String verifyImageAndResolveExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileExtensionException("파일이 비어 있습니다.");
        }

        byte[] header = new byte[ImageType.maxSignatureLength()];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(header, 0, header.length);
            if (read < header.length) {
                header = java.util.Arrays.copyOf(header, read);
            }
        } catch (IOException e) {
            throw new InvalidFileExtensionException("파일을 읽을 수 없습니다.");
        }

        ImageType type = ImageType.detect(header);
        if (type == null) {
            log.warn("이미지가 아닌 파일 업로드 시도 - 클라이언트가 주장한 타입={}, 파일명={}",
                    file.getContentType(), file.getOriginalFilename());
            throw new InvalidFileExtensionException();
        }
        return type.getExtension();
    }
}
