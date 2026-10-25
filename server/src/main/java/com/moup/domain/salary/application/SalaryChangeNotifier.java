package com.moup.domain.salary.application;

import com.moup.domain.alarm.domain.AlarmContent;
import com.moup.domain.alarm.domain.AlarmTitle;
import com.moup.domain.user.domain.User;
import com.moup.domain.workplace.domain.Workplace;
import com.moup.domain.workplace.exception.WorkplaceNotFoundException;
import com.moup.domain.workplace.mapper.WorkplaceRepository;
import com.moup.global.infra.fcm.FCMService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/// 급여 설정을 한쪽이 바꾸면 다른 쪽에 알린다 (Phase 11 D11-4).
///
/// 알바생과 사장님이 같은 설정을 고칠 수 있으므로 서로 다르게 고치면 나중 값이 이긴다.
/// 그 사실을 상대가 모르면 "내가 넣은 시급이 왜 바뀌었지"가 되므로 바뀔 때마다 알린다.
/// 같은 값을 다시 보낸 경우는 알리지 않는다 — 호출자가 `SalaryUpdateService.update`의 결과로 거른다.
@Component
@RequiredArgsConstructor
public class SalaryChangeNotifier {

    private final WorkplaceRepository workplaceRepository;
    private final FCMService fcmService;

    /// 알바생이 자기 급여 설정을 바꿨다 → 사장님에게.
    public void notifyOwner(User worker, Long workplaceId) {
        Workplace workplace = workplaceRepository.findById(workplaceId).orElseThrow(WorkplaceNotFoundException::new);
        Long ownerId = workplace.getOwnerId();
        if (ownerId == null || Objects.equals(ownerId, worker.getId())) {
            return;
        }
        fcmService.sendToSingleUser(worker.getId(), ownerId,
                AlarmTitle.ALARM_TITLE_SALARY_UPDATED.getTitle(),
                AlarmContent.ALARM_CONTENT_SALARY_UPDATED_BY_WORKER.getContent(
                        worker.getNickname(), workplace.getWorkplaceName()),
                null);
    }

    /// 사장님이 알바생 급여 설정을 바꿨다 → 알바생에게.
    public void notifyWorker(Long ownerUserId, Long workerUserId, Workplace workplace) {
        if (workerUserId == null || Objects.equals(workerUserId, ownerUserId)) {
            return;
        }
        fcmService.sendToSingleUser(ownerUserId, workerUserId,
                AlarmTitle.ALARM_TITLE_SALARY_UPDATED.getTitle(),
                AlarmContent.ALARM_CONTENT_SALARY_UPDATED_BY_OWNER.getContent(workplace.getWorkplaceName()),
                null);
    }
}
