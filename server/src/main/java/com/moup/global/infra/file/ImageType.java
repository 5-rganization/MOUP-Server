package com.moup.global.infra.file;

import lombok.Getter;

/// 매직 바이트로 판별하는 이미지 종류.
///
/// 클라이언트가 보내는 `Content-Type`과 원본 파일명은 **전부 클라이언트가 정한다.**
/// 그것만 믿으면 확장자만 `.png`로 바꾼 아무 파일이나 올릴 수 있고,
/// 원본 확장자를 그대로 이어붙여 저장하면 S3가 무료 파일 호스팅이 된다.
@Getter
public enum ImageType {
    JPEG(".jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG(".png", "image/png",
            new byte[]{(byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A}),
    GIF(".gif", "image/gif", new byte[]{'G', 'I', 'F', '8'});

    private final String extension;
    private final String contentType;
    private final byte[] signature;

    ImageType(String extension, String contentType, byte[] signature) {
        this.extension = extension;
        this.contentType = contentType;
        this.signature = signature;
    }

    boolean matches(byte[] header) {
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    /// 헤더 바이트로 이미지 종류를 판별한다. 알려진 종류가 아니면 `null`.
    public static ImageType detect(byte[] header) {
        for (ImageType type : values()) {
            if (type.matches(header)) {
                return type;
            }
        }
        return null;
    }

    /// 가장 긴 시그니처 길이. 이만큼만 읽으면 판별에 충분하다.
    public static int maxSignatureLength() {
        int max = 0;
        for (ImageType type : values()) {
            max = Math.max(max, type.signature.length);
        }
        return max;
    }
}
