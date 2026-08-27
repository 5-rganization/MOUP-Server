package com.moup.global.util;

import com.moup.global.error.InvalidPermissionAccessException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PermissionVerifyUtil {

    /// `workers.user_id`와 `workplaces.owner_id`는 스키마상 NULL을 허용한다
    /// (`db/moup.sql:95`, `:108`). 탈퇴한 사용자의 행은 `ON DELETE SET NULL`로
    /// NULL이 되므로 `equals` 직접 호출은 NPE(500)가 된다.
    /// `Objects.equals`로 NULL을 "불일치"로 취급해 fail-closed(403)로 만든다.

    public void verifyWorkerPermission(Long requesterUserId, Long workerUserId, Long workplaceOwnerId) {
        // 요청자가 해당 근무지의 근무자(사장님 포함)가 아니면 예외 발생
        if (!Objects.equals(workerUserId, requesterUserId) && !Objects.equals(workplaceOwnerId, requesterUserId)) {
            throw new InvalidPermissionAccessException();
        }
    }

    public void verifyOwnerPermission(Long requesterUserId, Long workplaceOwnerId) {
        // 요청자가 해당 매장의 등록자가 아니면 예외 발생
        if (!Objects.equals(workplaceOwnerId, requesterUserId)) {
            throw new InvalidPermissionAccessException();
        }
    }
}
