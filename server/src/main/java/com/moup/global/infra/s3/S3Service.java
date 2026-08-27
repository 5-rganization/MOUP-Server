package com.moup.global.infra.s3;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
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

    /// `extension`은 파일 내용으로 판별한 값을 받는다.
    /// 원본 파일명의 확장자를 그대로 쓰면 클라이언트가 저장 경로의 확장자를 마음대로 정하게 된다.
    public String saveFile(MultipartFile file, String extension) throws IOException, NoSuchAlgorithmException {
        String originalFileName = file.getOriginalFilename();
        String dataToHash = originalFileName + System.currentTimeMillis();

        // SHA-256 해싱
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        String fileName = sb.toString() + extension;
        String fileUrl = "https://" + bucket + ".s3.amazonaws.com/" + fileName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        amazonS3Client.putObject(bucket, fileName, file.getInputStream(), metadata);
        return fileUrl;
    }

    private String extractKeyFromUrl(String fileUrl) {
        return fileUrl.substring(fileUrl.indexOf(".amazonaws.com/") + ".amazonaws.com/".length());
    }

    public void deleteFile(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);
        amazonS3Client.deleteObject(new DeleteObjectRequest(bucket, key));
    }

    public boolean doesFileExist(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);
        return amazonS3Client.doesObjectExist(bucket, key);
    }

}
