package com.moup.server.common;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum File {
    JPEG("image/jpeg"),
    PNG("image/png"),
    GIF("image/gif"),
    IMAGE("image/"); // 모든 이미지 파일을 포괄하는 타입

    private final String contentType;

    File(String contentType) {
        this.contentType = contentType;
    }

}
