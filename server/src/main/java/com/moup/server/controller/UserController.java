package com.moup.server.controller;

import com.moup.server.model.dto.*;
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

        UserProfileResponse userProfileResponse = UserProfileResponse.builder().userId(userId).username(user.getUsername())
                .nickname(user.getNickname()).profileImg(user.getProfileImg()).role(user.getRole())
                .createdAt(user.getCreatedAt()).build();

        return ResponseEntity.ok().body(userProfileResponse);
    }

    @PatchMapping("/nickname")
    @Operation(summary = "닉네임 변경", description = "유저의 닉네임 변경")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserUpdateNicknameResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 닉네임", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 삭제 처리된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> updateNickname(@RequestBody UserUpdateNicknameRequest userUpdateNicknameRequest) {
        Long userId = identityService.getCurrentUserId();

        UserUpdateNicknameResponse userUpdateNicknameResponse = userService.updateNicknameByUserId(userId, userUpdateNicknameRequest.getNickname());

        return ResponseEntity.ok().body(userUpdateNicknameResponse);
    }

    @PatchMapping("/fcm-token")
    @Operation(summary = "FCM 토큰 갱신", description = "유저의 FCM 토큰 갱신")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "갱신 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserUpdateFCMTokenResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "이미 삭제 처리된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> updateFCMToken(@RequestBody UserUpdateFCMTokenRequest userUpdateFCMTokenRequest) {
        Long userId = identityService.getCurrentUserId();

        userService.updateFCMTokenByUserId(userId, userUpdateFCMTokenRequest.getFcmToken());

        return ResponseEntity.ok().body(UserUpdateFCMTokenResponse.builder().userId(userId).build());
    }

    @PatchMapping("/logout")
    @Operation(summary = "로그아웃", description = "유저 로그아웃")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그아웃 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserLogoutResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "이미 삭제 처리된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> logout() {
        Long userId = identityService.getCurrentUserId();

        userService.logout(userId);

        return ResponseEntity.ok().body(UserLogoutResponse.builder().userId(userId).build());
    }

    @DeleteMapping()
    @Operation(summary = "유저 탈퇴", description = "현재 로그인된 유저의 계정을 탈퇴 (3일의 유예 기간 존재)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유저 삭제 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDeleteResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 삭제 처리된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<?> deleteUser() {
        Long userId = identityService.getCurrentUserId();

        UserDeleteResponse userDeleteResponse = userService.deleteUserSoftlyByUserId(userId);

        return ResponseEntity.ok().body(userDeleteResponse);
    }
}
