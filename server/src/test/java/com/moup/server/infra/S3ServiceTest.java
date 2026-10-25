package com.moup.server.infra;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.moup.global.infra.file.ImageType;
import com.moup.global.infra.s3.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/// S3 저장 메타데이터와 URL → 키 변환 (스코프 7 I7 · Minor).
@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock private AmazonS3Client amazonS3Client;
    @InjectMocks private S3Service s3Service;

    @Test
    @DisplayName("S3에는 클라이언트가 보낸 Content-Type이 아니라 판별한 종류가 저장된다")
    void 판별한_콘텐츠_타입으로_저장() throws Exception {
        ReflectionTestUtils.setField(s3Service, "bucket", "moup-bucket");
        MockMultipartFile polyglot = new MockMultipartFile("file", "a.png", "text/html", new byte[]{1, 2, 3});

        String url = s3Service.saveFile(polyglot, ImageType.PNG);

        ArgumentCaptor<ObjectMetadata> metadata = ArgumentCaptor.forClass(ObjectMetadata.class);
        verify(amazonS3Client).putObject(eq("moup-bucket"), anyString(), any(InputStream.class), metadata.capture());
        assertEquals("image/png", metadata.getValue().getContentType());
        assertTrue(url.endsWith(".png"));
    }

    /// 예전에는 `indexOf`가 -1이어도 잘라 URL 앞부분으로 만든 엉뚱한 키로 `deleteObject`가 나갔다.
    @Test
    @DisplayName("S3가 아닌 URL은 지우지 않고 존재하지 않는 것으로 본다")
    void S3가_아닌_URL() {
        s3Service.deleteFile("https://lh3.googleusercontent.com/a/photo.jpg");

        assertFalse(s3Service.doesFileExist("https://lh3.googleusercontent.com/a/photo.jpg"));
        verifyNoInteractions(amazonS3Client);
    }
}
