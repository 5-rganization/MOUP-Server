package com.moup.domain.alarm.api;

import com.moup.global.common.response.ErrorResponse;
import com.moup.domain.alarm.domain.Announcement;
import com.moup.domain.alarm.domain.Notification;
import com.moup.domain.alarm.application.AlarmService;
import com.moup.global.security.IdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Alarm-Controller", description = "알림 관리 API 엔드포인트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/alarms")
public class AlarmController {

    private final IdentityService identityService;
    private final AlarmService alarmService;

    @GetMapping("/announcements")
    @Operation(summary = "전체 공지 일괄 조회", description = "전체 공지를 일괄 조회하여 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Announcement.class)))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "조회 결과 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> findAllAnnouncements() {
        Long userId = identityService.getCurrentUserId();

        List<Announcement> announcementList = alarmService.findAllAnnouncements(userId);

        return ResponseEntity.ok().body(announcementList);
    }

    @GetMapping("/announcements/{announcementId}")
    @Operation(summary = "전체 공지 조회", description = "전체 공지 id를 기준으로 공지를 조회하여 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Announcement.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "조회 결과 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> findAnnouncement(@PathVariable Long announcementId) {
        Long userId = identityService.getCurrentUserId();

        Announcement announcementList = alarmService.findAnnouncementById(userId, announcementId);

        return ResponseEntity.ok().body(announcementList);
    }

    @PatchMapping("/announcements/{announcementId}/read")
    @Operation(summary = "전체 공지 읽음 처리", description = "전체 공지 id를 기준으로 공지를 읽음 처리합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "읽음 처리 성공", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> readAnnouncement(@PathVariable Long announcementId) {
        Long userId = identityService.getCurrentUserId();

        alarmService.readAnnouncementById(userId, announcementId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/announcements/read")
    @Operation(summary = "전체 공지 일괄 읽음 처리", description = "전체 공지를 일괄 읽음 처리합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "읽음 처리 성공", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> readAllAnnouncements() {
        Long userId = identityService.getCurrentUserId();

        alarmService.readAllAnnouncements(userId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/announcements/{announcementId}")
    @Operation(summary = "전체 공지 삭제", description = "전체 공지 id를 기준으로 공지를 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long announcementId) {
        Long userId = identityService.getCurrentUserId();

        alarmService.deleteAnnouncementById(userId, announcementId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/announcements")
    @Operation(summary = "전체 공지 일괄 삭제", description = "전체 공지들을 일괄 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
        @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> deleteAllAnnouncements() {
        Long userId = identityService.getCurrentUserId();

        alarmService.deleteAllAnnouncements(userId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/notifications")
    @Operation(summary = "일반 알림 일괄 조회", description = "일반 알림을 일괄 조회하여 반환합니다.(읽음 처리된 알림 포함)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Notification.class)))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "조회 결과 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> findAllNotifications() {
        Long userId = identityService.getCurrentUserId();

        List<Notification> notificationList = alarmService.findAllNotifications(userId);

        return ResponseEntity.ok().body(notificationList);
    }

    @GetMapping("/notifications/{notificationId}")
    @Operation(summary = "일반 알림 조회", description = "일반 알림 id를 기준으로 알림을 조회하여 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Notification.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "조회 결과 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> findNotification(@PathVariable Long notificationId) {
        Long userId = identityService.getCurrentUserId();

        Notification notification = alarmService.findNotificationById(userId, notificationId);

        return ResponseEntity.ok().body(notification);
    }

    @PatchMapping("/notifications/{notificationId}/read")
    @Operation(summary = "일반 알림 읽음 처리", description = "일반 알림 id를 기준으로 알림을 읽음 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Notification.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "조회 결과 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 읽음 처리된 알림", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> readNotification(@PathVariable Long notificationId) {
        Long userId = identityService.getCurrentUserId();

        Notification notification = alarmService.readNotificationById(userId, notificationId);

        return ResponseEntity.ok().body(notification);
    }

    @PatchMapping("/notifications/read")
    @Operation(summary = "일반 알림 일괄 읽음 처리", description = "일반 알림을 일괄 읽음 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> readAllNotifications() {
        Long userId = identityService.getCurrentUserId();

        alarmService.readAllNotification(userId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/notifications/{notificationId}")
    @Operation(summary = "일반 알림 삭제", description = "일반 알림 id를 기준으로 알림을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "조회 결과 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> deleteNotification(@PathVariable Long notificationId) {
        Long userId = identityService.getCurrentUserId();

        alarmService.deleteNotificationById(userId, notificationId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/notifications")
    @Operation(summary = "일반 알림 일괄 삭제", description = "일반 알림들을 일괄 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 토큰 없음 또는 유효하지 않음"),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    public ResponseEntity<?> deleteAllNotifications() {
        Long userId = identityService.getCurrentUserId();

        alarmService.deleteAllNotifications(userId);

        return ResponseEntity.ok().build();
    }
}
