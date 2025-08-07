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

@RequestMapping("/api/admin")
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @DeleteMapping("/users/hard-delete")
    @Operation(summary = "오래된 유저 하드 삭제", description = "3일 이상 소프트 삭제 상태인 유저를 영구 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PreAuthorize("hasRole('ADMIN')") // 관리자 권한 필요
    public ResponseEntity<Void> hardDeleteOldUsers() {
        adminService.hardDeleteOldUsers();
        return ResponseEntity.ok().build();
    }
}
