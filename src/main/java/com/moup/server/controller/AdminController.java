package com.moup.server.controller;

import com.moup.server.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/admin")
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @DeleteMapping("/users")
    @Operation(summary = "유저 DB 영구 삭제", description = "유예 기간 이상 삭제 상태인 유저를 DB에서 영구 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')") // 관리자 권한 필요
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
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> hardDeleteUsersImmediately() {
        adminService.hardDeleteUsersImmediately();
        return ResponseEntity.noContent().build();
    }
}
