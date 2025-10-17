package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.service.IdentityService;
import com.moup.server.service.UserService;
import com.moup.server.service.WorkplaceService;
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
            WorkplaceSummaryResponse response = workplaceService.getSummarizedWorkplace(userId, workplaceId);
            return ResponseEntity.ok().body(response);
        }

        BaseWorkplaceDetailResponse response = workplaceService.getWorkplaceDetail(user, workplaceId);
        return ResponseEntity.ok().body(response);
    }

    @Override
    @GetMapping("/summary")
    public ResponseEntity<?> getAllSummarizedWorkplace(
            @Parameter(name = "isShared", description = "공유 근무지(매장) 조회 여부", in = ParameterIn.QUERY)
            @RequestParam(name = "isShared", required = false) Boolean isShared
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        List<WorkplaceSummaryResponse> summaryResponseList = workplaceService.getAllSummarizedWorkplace(user.getId(), isShared);

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
    public ResponseEntity<?> deleteWorkplace(
            @PathVariable @Positive(message = "1 이상의 값만 입력해야 합니다.") Long workplaceId
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        workplaceService.deleteWorkplace(user.getId(), workplaceId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PutMapping("/invite-code/{workplaceId}")
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

    @Override
    @GetMapping("/invite-code/{inviteCode}")
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
    @PostMapping("/invite-code/{inviteCode}")
    @PreAuthorize("hasRole('ROLE_WORKER')")
    public ResponseEntity<?> joinWorkplace(
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9]{6}$", message = "초대 코드는 영문 또는 숫자로 이루어진 6자리여야 합니다.") String inviteCode,
            @RequestBody @Valid WorkplaceJoinRequest request
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        WorkplaceJoinResponse response = workplaceService.joinWorkplace(user, inviteCode, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/workplaces/{id}/workers/{workerId}")
                .buildAndExpand(response.getWorkplaceId(), response.getWorkerId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
