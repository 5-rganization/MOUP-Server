package com.moup.server.controller;

import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.service.IdentityService;
import com.moup.server.service.UserService;
import com.moup.server.service.WorkService;
import com.moup.server.service.WorkplaceService;
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
            @RequestParam(name = "view", required = false, defaultValue = "DETAIL") ViewType view
    ) {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        return switch (view) {
            case SUMMARY -> {
                WorkplaceSummaryResponse response = workplaceService.getWorkplace(userId, workplaceId);
                yield ResponseEntity.ok().body(response);
            }
            case DETAIL -> {
                BaseWorkplaceDetailResponse response = workplaceService.getWorkplaceDetail(user, workplaceId);
                yield ResponseEntity.ok().body(response);
            }
        };
    }

    @Override
    @GetMapping
    public ResponseEntity<?> getAllWorkplace(
            @RequestParam(name = "isSharedOnly", required = false, defaultValue = "false") boolean isSharedOnly
    ) {
        Long userId = identityService.getCurrentUserId();

        List<WorkplaceSummaryResponse> summaryResponseList = workplaceService.getAllWorkplace(userId, isSharedOnly);

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

        workplaceService.deleteWorkplace(userId, workplaceId);
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
}
