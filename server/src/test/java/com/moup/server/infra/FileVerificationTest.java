package com.moup.server.infra;

import com.moup.global.error.InvalidFileExtensionException;
import com.moup.global.infra.file.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Phase 8 회귀 테스트 — 업로드 파일을 **내용으로** 검증한다 (스코프 7 I7).
///
/// `Content-Type`과 원본 파일명은 전부 클라이언트가 정하는 값이다.
/// 예전에는 `Content-Type`이 `image/`로 시작하는지만 보고, 저장할 때는 원본 확장자를
/// 그대로 이어붙였다. **S3가 무료 파일 호스팅이 됐다.**
class FileVerificationTest {

    private final FileService fileService = new FileService();

    private static final byte[] PNG_HEADER =
            {(byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};
    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    private static final byte[] GIF_HEADER = {'G', 'I', 'F', '8', '9', 'a'};

    @Test
    @DisplayName("확장자와 Content-Type을 위조해도 내용이 이미지가 아니면 거부한다")
    void 위조된_이미지는_거부된다() {
        MockMultipartFile fake = new MockMultipartFile(
                "file", "payload.png", "image/png",
                "<html><script>alert(1)</script></html>".getBytes());

        assertThrows(InvalidFileExtensionException.class,
                () -> fileService.verifyImageAndResolveExtension(fake));
    }

    @Test
    @DisplayName("진짜 이미지는 통과하고, 확장자는 원본 파일명이 아니라 내용에서 나온다")
    void 확장자는_내용에서_결정된다() {
        MockMultipartFile png = new MockMultipartFile(
                "file", "misleading-name.gif", "image/gif", PNG_HEADER);

        assertEquals(".png", fileService.verifyImageAndResolveExtension(png),
                "원본 확장자를 그대로 쓰면 클라이언트가 저장 경로의 확장자를 정하게 된다");
    }

    @Test
    @DisplayName("JPEG · GIF도 매직 바이트로 판별한다")
    void 다른_포맷도_판별한다() {
        assertEquals(".jpg", fileService.verifyImageAndResolveExtension(
                new MockMultipartFile("file", "a", "application/octet-stream", JPEG_HEADER)));
        assertEquals(".gif", fileService.verifyImageAndResolveExtension(
                new MockMultipartFile("file", "a", "application/octet-stream", GIF_HEADER)));
    }

    @Test
    @DisplayName("빈 파일은 거부한다")
    void 빈_파일은_거부() {
        assertThrows(InvalidFileExtensionException.class,
                () -> fileService.verifyImageAndResolveExtension(
                        new MockMultipartFile("file", "a.png", "image/png", new byte[0])));
    }

    @Test
    @DisplayName("시그니처보다 짧은 파일도 안전하게 거부한다")
    void 너무_짧은_파일도_거부() {
        assertThrows(InvalidFileExtensionException.class,
                () -> fileService.verifyImageAndResolveExtension(
                        new MockMultipartFile("file", "a.png", "image/png", new byte[]{(byte) 0x89, 'P'})));
    }
}
