package com.moup.global.infra.file;

import lombok.Getter;

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
