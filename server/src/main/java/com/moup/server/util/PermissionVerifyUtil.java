package com.moup.server.util;

import com.moup.server.exception.InvalidPermissionAccessException;
import org.springframework.stereotype.Component;

@Component
public class PermissionVerifyUtil {
    public void verifyWorkerPermission(Long requesterUserId, Long workerUserId, Long workplaceOwnerId) {
        // 요청자가 해당 근무지의 근무자(사장님 포함)가 아니면 예외 발생
        if (!workerUserId.equals(requesterUserId) && !workplaceOwnerId.equals(requesterUserId)) {
            throw new InvalidPermissionAccessException();
        }
    }

    public void verifyOwnerPermission(Long requesterUserId, Long workplaceOwnerId) {
        // 요청자가 해당 매장의 등록자가 아니면 예외 발생
        if (!workplaceOwnerId.equals(requesterUserId)) {
            throw new InvalidPermissionAccessException();
        }
    }
}
