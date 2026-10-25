package com.moup.global.infra.s3;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.moup.global.infra.file.ImageType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /// `type`은 파일 내용으로 판별한 값을 받는다. 확장자와 Content-Type을 모두 여기서 정한다.
    /// 원본 파일명의 확장자를 쓰면 클라이언트가 저장 경로의 확장자를 정하게 되고, 클라이언트의 Content-Type을 쓰면
    /// 이미지 시그니처로 시작하는 HTML을 `text/html`로 서빙하게 된다(스코프 7 I7).
    public String saveFile(MultipartFile file, ImageType type) throws IOException, NoSuchAlgorithmException {
        String originalFileName = file.getOriginalFilename();
        String dataToHash = originalFileName + System.currentTimeMillis();

        // SHA-256 해싱
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        String fileName = sb.toString() + type.getExtension();
        String fileUrl = "https://" + bucket + ".s3.amazonaws.com/" + fileName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(type.getContentType());
        metadata.setContentLength(file.getSize());

        amazonS3Client.putObject(bucket, fileName, file.getInputStream(), metadata);
        return fileUrl;
    }

    private static final String S3_HOST_SUFFIX = ".amazonaws.com/";

    /// S3 URL이 아니면 `null`. 예전에는 `indexOf`가 -1이어도 그대로 잘라, S3가 아닌 URL(소셜 프로필 사진 등)을
    /// 지우려 하면 **URL 앞부분으로 만든 엉뚱한 키**로 `deleteObject`가 나갔다(스코프 7 Minor).
    private String extractKeyFromUrl(String fileUrl) {
        int index = fileUrl == null ? -1 : fileUrl.indexOf(S3_HOST_SUFFIX);
        return index < 0 ? null : fileUrl.substring(index + S3_HOST_SUFFIX.length());
    }

    public void deleteFile(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);
        if (key == null) {
            return;
        }
        amazonS3Client.deleteObject(new DeleteObjectRequest(bucket, key));
    }

    public boolean doesFileExist(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);
        return key != null && amazonS3Client.doesObjectExist(bucket, key);
    }

}
