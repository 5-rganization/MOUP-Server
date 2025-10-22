package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.service.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.YearMonth;
import java.util.List;

/**
 * @author neoskyclad
 * <p>
 * 근무지 정보 관리를 위한 Controller
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/workplaces")
public class WorkplaceController implements WorkplaceSpecification, InviteCodeSpecification {
    private final UserService userService;
    private final IdentityService identityService;
    private final WorkplaceService workplaceService;
    private final WorkService workService;

    @Override
    @PostMapping
    public ResponseEntity<?> createWorkplace(@RequestBody @Valid BaseWorkplaceCreateRequest request) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        WorkplaceCreateResponse response = workplaceService.createWorkplace(user, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getWorkplaceId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    @GetMapping("/{workplaceId}")
    public ResponseEntity<?> getWorkplace(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestParam(name = "view", required = false) ViewType view
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        if (view == ViewType.SUMMARY) {
            WorkplaceSummaryResponse response = workplaceService.getWorkplace(userId, workplaceId);
            return ResponseEntity.ok().body(response);
        }

        BaseWorkplaceDetailResponse response = workplaceService.getWorkplaceDetail(user, workplaceId);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<?> getAllWorkplace(
            @Parameter(name = "isShared", description = "공유 근무지(매장) 조회 여부", in = ParameterIn.QUERY)
            @RequestParam(name = "isShared", required = false) Boolean isShared
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        List<WorkplaceSummaryResponse> summaryResponseList = workplaceService.getAllWorkplace(user.getId(), isShared);

        WorkplaceSummaryListResponse response = WorkplaceSummaryListResponse.builder()
                .workplaceSummaryInfoList(summaryResponseList)
                .build();
        return ResponseEntity.ok().body(response);
    }

    @Override
    @PatchMapping("/{workplaceId}")
    public ResponseEntity<?> updateWorkplace(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid BaseWorkplaceUpdateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        workplaceService.updateWorkplace(user, workplaceId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{workplaceId}")
    public ResponseEntity<?> deleteWorkplace(@PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        workplaceService.deleteWorkplace(user.getId(), workplaceId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PutMapping("{workplaceId}/invite-code")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    public ResponseEntity<?> generateInviteCode(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestBody @Valid InviteCodeGenerateRequest request
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        InviteCodeGenerateResponse response = workplaceService.generateInviteCode(user, workplaceId, request);
        if (response.getReturnAlreadyExists()) {
            return ResponseEntity.ok().body(response);
        } else {
            URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/workplaces/invite-code/{inviteCode}")
                    .buildAndExpand(response.getInviteCode())
                    .toUri();
            return ResponseEntity.created(location).body(response);
        }
    }

    // TODO: 근무지 초대 승인 및 알바생에게 승인 푸시 알림 구현

    // TODO: 사장님 매장 근무자 수정 기능 구현

    @Override
    @GetMapping("/invite-codes/{inviteCode}")
    @PreAuthorize("hasRole('ROLE_WORKER')")
    public ResponseEntity<?> inquireInviteCode(
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9]{6}$", message = "초대 코드는 영문 또는 숫자로 이루어진 6자리여야 합니다.") String inviteCode
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        InviteCodeInquiryResponse response = workplaceService.inquireInviteCode(user, inviteCode);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @PostMapping("/join")
    @PreAuthorize("hasRole('ROLE_WORKER')")
    public ResponseEntity<?> joinWorkplace(@RequestBody @Valid WorkplaceJoinRequest request) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        WorkplaceJoinResponse response = workplaceService.joinWorkplace(user, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/workplaces/{id}/workers/{workerId}")
                .buildAndExpand(response.getWorkplaceId(), response.getWorkerId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    @GetMapping("/{workplaceId}/works")
    public ResponseEntity<?> getAllWorkByWorkplace(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId,
            @RequestParam(name = "baseYearMonth") YearMonth baseYearMonth
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        WorkCalendarListResponse response = workService.getAllWorkByWorkplace(user, workplaceId, baseYearMonth);
        return ResponseEntity.ok().body(response);
    }
}
