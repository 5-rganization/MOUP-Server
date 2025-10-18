package com.moup.server.util;

import com.moup.server.exception.InvalidPermissionAccessException;
import org.springframework.stereotype.Component;

@Component
public class PermissionVerifyUtil {
    public void verifyWorkerOrOwnerPermission(Long requesterUserId, Long workerUserId, Long workplaceOwnerId) {
        // 요청자가 해당 근무지의 근무자가 아니면서 사장님도 아니면 예외 발생
        if (!workerUserId.equals(requesterUserId) && !workplaceOwnerId.equals(requesterUserId)) {
            throw new InvalidPermissionAccessException();
        }
    }

    public void verifyWorkerPermission(Long requesterUserId, Long workerUserId) {
        // 요청자가 해당 근무지의 근무자가 아니면 예외 발생
        if (!workerUserId.equals(requesterUserId)) {
            throw new InvalidPermissionAccessException();
        }
    }

    public void verifyOwnerPermission(Long requesterUserId, Long workplaceOwnerId) {
        // 요청자가 해당 매장의 사장이 아니면 예외 발생
        if (!workplaceOwnerId.equals(requesterUserId)) {
            throw new InvalidPermissionAccessException();
        }
    }
}
