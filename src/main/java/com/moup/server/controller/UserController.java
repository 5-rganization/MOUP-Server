package com.moup.server.controller;

import com.moup.server.model.dto.ErrorResponse;
import com.moup.server.model.dto.UserProfileResponse;
import com.moup.server.model.dto.UserDeleteResponse;
import com.moup.server.model.dto.UserRestoreResponse;
import com.moup.server.model.entity.User;
import com.moup.server.service.IdentityService;
import com.moup.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author neoskyclad
 * <p>
 * 유저 정보 관리를 위한 Controller
 */
@Tag(name = "User-Controller", description = "유저 정보 관리 API 엔드포인트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final IdentityService identityService;

    @GetMapping("/profiles")
    @Operation(summary = "프로필 조회", description = "현재 로그인된 유저의 프로필 이미지, 닉네임, 역할 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 삭제 처리된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<?> getUserProfile() {
        Long userId = identityService.getCurrentUserId();

        User user = userService.findUserById(userId);

        UserProfileResponse userProfileResponse = UserProfileResponse.builder().username(user.getUsername())
                .nickname(user.getNickname()).profileImg(user.getProfileImg()).role(user.getRole())
                .createdAt(user.getCreatedAt()).build();

        return ResponseEntity.ok().body(userProfileResponse);
    }

    @DeleteMapping()
    @Operation(summary = "유저 삭제", description = "현재 로그인된 유저의 계정을 삭제 (3일의 유예 기간 존재)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유저 삭제 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDeleteResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 삭제 처리된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<?> deleteUser() {
        Long userId = identityService.getCurrentUserId();

        UserDeleteResponse userDeleteResponse = userService.deleteSoftUserByUserId(userId);

        return ResponseEntity.ok().body(userDeleteResponse);
    }

    @PutMapping("/restore")
    @Operation(summary = "삭제 철회", description = "삭제 처리된 유저의 삭제 철회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 철회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserRestoreResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 복원된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> restoreUser() {
        Long userId = identityService.getCurrentUserId();

        UserRestoreResponse userRestoreResponse = userService.restoreUserByUserId(userId);

        return ResponseEntity.ok().body(userRestoreResponse);
    }
}
