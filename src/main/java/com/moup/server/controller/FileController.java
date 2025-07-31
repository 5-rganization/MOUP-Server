package com.moup.server.controller;

import com.moup.server.exception.InvalidFileExtensionException;
import com.moup.server.model.entity.User;
import com.moup.server.service.IdentityService;
import com.moup.server.service.S3Service;
import com.moup.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * @author neoskyclad
 *
 * 파일 관리를 위한 Controller
 */
@Tag(name = "File-Controller", description = "파일 관리 API 엔드포인트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final UserService userService;
    private final IdentityService identityService;
    private final S3Service s3Service;

    @PostMapping(value = "/users/profile-images", consumes = "multipart/form-data")
    @Operation(summary = "프로필 이미지 업로드", description = "현재 로그인된 유저의 프로필 이미지를 갱신")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 이미지 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저"),
            @ApiResponse(responseCode = "500", description = "서버 오류"),
    })
    public ResponseEntity<?> uploadUserProfileImage(
            @Parameter(description = "업로드할 프로필 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        // 이미지 타입인지 파일 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidFileExtensionException();
        }
        
        Long userId = identityService.getCurrentUserId();
        User user = userService.findByUserId(userId);

        // 기존 이미지가 있다면 해당 파일 삭제
        if (user.getProfileImg() != null && s3Service.doesFileExist(user.getProfileImg())) {
            s3Service.deleteFile(user.getProfileImg());
        }

        // 새 이미지 업로드하기
        String imageUrl = "";
        try {
            imageUrl = s3Service.saveFile(file);
            userService.updateProfileImg(userId, imageUrl);
        } catch (Exception e) {
            // 500 에러 던지기
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }
}
