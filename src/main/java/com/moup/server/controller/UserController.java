package com.moup.server.controller;

import com.moup.server.model.dto.UserProfileResponse;
import com.moup.server.model.entity.User;
import com.moup.server.service.IdentityService;
import com.moup.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author neoskyclad
 * 
 * 유저 정보 관리를 위한 Controller
 */
@Tag(name = "User-Controller", description = "유저 정보 관리 API 엔드포인트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final IdentityService identityService;

    @GetMapping("/profile")
    @Operation(summary = "프로필 조회", description = "현재 로그인된 유저의 프로필 이미지, 닉네임, 역할 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getUserProfile() {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findByUserId(userId);

        System.out.println(user.toString());

        UserProfileResponse userProfile = UserProfileResponse.builder()
                .username(user.getUsername())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(userProfile);
    }

//    @DeleteMapping()
//    public ResponseEntity<?> deleteUser() {
//
//    }
}
