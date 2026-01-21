package com.moup.domain.user.api;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.moup.domain.alarm.dto.AdminAnnouncementRequest;
import com.moup.domain.alarm.dto.AdminNotificationRequest;
import com.moup.domain.user.application.AdminService;
import com.moup.global.security.IdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/admin")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')") // 관리자 권한 필요
public class AdminController {
    
    // TODO: admin 패키지 없애고, 각 세부 도메인으로 이동시키고 관리자 api로 나누기

    private final AdminService adminService;
    private final IdentityService identityService;

    @DeleteMapping("/users")
    @Operation(summary = "유저 DB 영구 삭제", description = "유예 기간 이상 삭제 상태인 유저를 DB에서 영구 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Void> hardDeleteOldUsers() {
        adminService.hardDeleteOldUsers();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/immediate")
    @Operation(summary = "모든 삭제 상태 유저 즉시 영구 삭제", description = "유예 기간에 상관없이 모든 삭제 상태의 유저를 DB에서 즉시 영구 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Void> hardDeleteUsersImmediately() {
        adminService.hardDeleteUsersImmediately();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alarms/announcements")
    @Operation(summary = "전체 공지 푸시 알림 전송", description = "모든 유저에게 공지 사항을 푸시 알림으로 전송합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "전송 요청 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> announce(@RequestBody AdminAnnouncementRequest adminAnnouncementRequest)
        throws FirebaseMessagingException {
        adminService.announce(adminAnnouncementRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alarms/notification")
    @Operation(summary = "일반 알림 전송", description = "특정 유저에게 푸시 알림으로 전송합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "전송 요청 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "유저 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> notify(@RequestBody AdminNotificationRequest adminNotificationRequest)
        throws FirebaseMessagingException {
        Long userId = identityService.getCurrentUserId();

        adminService.notify(userId, adminNotificationRequest);
        return ResponseEntity.noContent().build();
    }
}
