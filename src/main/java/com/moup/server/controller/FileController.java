package com.moup.server.controller;

import com.moup.server.common.File;
import com.moup.server.model.dto.ErrorResponse;
import com.moup.server.model.dto.UserProfileImageResponse;
import com.moup.server.model.dto.UserProfileResponse;
import com.moup.server.model.entity.User;
import com.moup.server.service.FileService;
import com.moup.server.service.IdentityService;
import com.moup.server.service.S3Service;
import com.moup.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

/**
 * @author neoskyclad
 * <p>
 * 파일 관리를 위한 Controller
 */
@Tag(name = "File-Controller", description = "파일 관리 API 엔드포인트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final UserService userService;
    private final IdentityService identityService;

    @PostMapping(value = "/users/profile-images", consumes = "multipart/form-data")
    @Operation(summary = "프로필 이미지 업로드", description = "현재 로그인된 유저의 프로필 이미지를 갱신")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 이미지 업로드 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileImageResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "삭제 처리된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> uploadUserProfileImage(
            @Parameter(description = "업로드할 프로필 이미지 파일", required = true) @RequestParam("file") MultipartFile file) {

        Long userId = identityService.getCurrentUserId();

        UserProfileImageResponse userProfileImageResponse = userService.updateProfileImage(userId, file);

        return ResponseEntity.ok().body(userProfileImageResponse);
    }
}
