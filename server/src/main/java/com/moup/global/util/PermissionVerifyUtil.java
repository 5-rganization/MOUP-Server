package com.moup.global.util;

import com.moup.domain.user.domain.Worker;
import com.moup.domain.user.mapper.UserRepository;
import com.moup.global.error.InvalidPermissionAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/// 근무지 스코프 권한 검증.
///
/// `workers.user_id`와 `workplaces.owner_id`는 스키마상 NULL을 허용한다
/// (탈퇴 시 `ON DELETE SET NULL`). `equals`를 직접 호출하면 NPE(500)가 나므로
/// `Objects.equals`로 NULL을 "불일치"로 취급해 fail-closed(403)로 만든다.
@Component
@RequiredArgsConstructor
public class PermissionVerifyUtil {

    private final UserRepository userRepository;

    /// 근무 행위(등록·출퇴근·수정·삭제·조회)에 대한 검증.
    ///
    /// **승인(`is_accepted`)을 함께 확인한다.** 초대코드로 참여만 하면
    /// `is_accepted = false`인데, 이전에는 이 값을 읽는 곳이 코드베이스 전체에 0건이라
    /// **사장님의 승인 버튼이 아무것도 막지 못했다.** 코드를 손에 넣은 사람은
    /// 참여 요청 201을 받는 순간 승인된 근무자와 동일한 권한을 가졌다.
    ///
    /// 이름이 기본형인 이유는 **새 호출부가 실수로 골라도 안전한 쪽**이기 때문이다.
    /// 승인 대기 중에도 허용해야 하는 경우에만 아래 `verifyWorkerIdentityAllowingPending`을 쓴다.
    public void verifyWorkerPermission(Long requesterUserId, Worker worker, Long workplaceOwnerId) {
        // 사장님은 자기 근무지 알바생의 근무를 조회·수정·삭제할 수 있다.
        if (Objects.equals(workplaceOwnerId, requesterUserId)) {
            return;
        }
        verifyWorkerIdentityAllowingPending(requesterUserId, worker.getUserId(), workplaceOwnerId);
        if (!Boolean.TRUE.equals(worker.getIsAccepted())) {
            throw new InvalidPermissionAccessException("승인 대기 중인 근무지입니다.");
        }
    }

    /// 승인 여부를 따지지 않는 본인 확인.
    ///
    /// 승인 대기 중에도 허용해야 하는 것에만 쓴다 — 자기 급여 설정 조회·수정(정책 6 (b))과
    /// 참여 취소. **참여 취소를 막으면 대기자가 근무지에서 빠져나올 수 없다.**
    public void verifyWorkerIdentityAllowingPending(Long requesterUserId, Long workerUserId,
                                                    Long workplaceOwnerId) {
        if (!Objects.equals(workerUserId, requesterUserId)
                && !Objects.equals(workplaceOwnerId, requesterUserId)) {
            throw new InvalidPermissionAccessException();
        }
    }

    /// 사장님이 탈퇴한 근무지에서는 **쓰기만** 막는다 (확정 정책 5 — 데이터 보존, 접근만 차단).
    ///
    /// 조회는 허용한다. 근무·급여 기록은 사장님만의 것이 아니라 **알바생의 임금·소득
    /// 증빙**이기도 하다. 데이터를 보존하는 목적이 그것인데 정작 당사자가 못 보면
    /// 보존의 의미가 없다.
    ///
    /// 쓰기를 막는 이유는 승인할 사장님이 없어 근무·급여 변경이 무의미하기 때문이다.
    /// 근무지 **탈퇴**는 막지 않는다 — 막으면 알바생이 사라진 근무지에 영원히 묶인다.
    ///
    /// 소유자가 하드 삭제된 과거 데이터(`owner_id IS NULL`)도 같이 막는다.
    public void verifyWorkplaceIsWritable(Long workplaceOwnerId) {
        if (workplaceOwnerId == null || userRepository.isWithdrawn(workplaceOwnerId)) {
            throw new InvalidPermissionAccessException("사장님이 탈퇴한 근무지입니다. 기록 조회만 가능합니다.");
        }
    }

    public void verifyOwnerPermission(Long requesterUserId, Long workplaceOwnerId) {
        if (!Objects.equals(workplaceOwnerId, requesterUserId)) {
            throw new InvalidPermissionAccessException();
        }
    }
}
