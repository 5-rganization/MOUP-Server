package com.moup.domain.workplace.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class InviteCodeRepository {
    private static final String INVITE_CODE_KEY_PREFIX = "inviteCode:";
    private static final String WORKPLACE_ID_KEY_PREFIX = "workplaceId:";
    private static final String ATTEMPT_KEY_PREFIX = "inviteAttempt:";

    private final StringRedisTemplate stringRedisTemplate;

    /// 초대 코드를 그 근무지 것으로 **선점**한다. 이미 다른 근무지가 쓰는 코드면 아무것도 바꾸지 않고 `false`.
    ///
    /// 예전에는 `existsByInviteCode`로 확인한 뒤 따로 `save`해서, 두 요청이 같은 코드를 동시에 뽑으면
    /// 나중 요청이 앞 근무지의 매핑을 **말없이 덮어써** 코드가 엉뚱한 근무지를 가리켰다(스코프 5 I-5).
    /// 코드 쪽 키를 SET NX로 잡고, 잡은 경우에만 역방향 매핑을 쓴다.
    public boolean claim(String inviteCode, Long workplaceId, Duration ttl) {
        Boolean claimed = stringRedisTemplate.opsForValue()
                .setIfAbsent(INVITE_CODE_KEY_PREFIX + inviteCode, workplaceId.toString(), ttl);
        if (!Boolean.TRUE.equals(claimed)) {
            return false;
        }
        stringRedisTemplate.opsForValue().set(WORKPLACE_ID_KEY_PREFIX + workplaceId, inviteCode, ttl);
        return true;
    }

    /**
     * 근무지 ID에 연결된 초대 코드를 조회하는 메서드
     * @param workplaceId 조회할 근무지 ID
     * @return 초대 코드를 포함한 Optional 객체, 코드가 없으면 Optional.empty()
     */
    public Optional<String> findInviteCodeByWorkplaceId(Long workplaceId) {
        String key = WORKPLACE_ID_KEY_PREFIX + workplaceId;
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(key));
    }

    /**
     * 초대 코드에 연결된 근무지 ID를 조회하는 메서드
     * @param inviteCode 조회할 6자리 초대 코드
     * @return 근무지 ID를 포함한 Optional 객체, 코드가 없으면 Optional.empty()
     */
    public Optional<Long> findWorkplaceIdByInviteCode(String inviteCode) {
        String key = INVITE_CODE_KEY_PREFIX + inviteCode;
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(key)).map(Long::parseLong);
    }

    /**
     * 초대 코드 조회 **실패** 1회를 기록하고 창 안의 누적 실패 횟수를 반환한다.
     *
     * 성공한 조회는 세지 않는다. 정상 사용자는 코드를 한두 번 잘못 입력할 뿐이고,
     * 무차별 대입은 정의상 실패가 압도적으로 많다.
     *
     * @param userId 요청자 ID
     * @param windowMinutes 카운터 유지 시간(분)
     * @return 창 안의 누적 실패 횟수
     */
    public long recordFailedAttempt(Long userId, int windowMinutes) {
        String key = ATTEMPT_KEY_PREFIX + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, windowMinutes, TimeUnit.MINUTES);
        }
        return count == null ? 0L : count;
    }

    /**
     * 창 안의 누적 실패 횟수를 조회한다.
     */
    public long countFailedAttempts(Long userId) {
        String value = stringRedisTemplate.opsForValue().get(ATTEMPT_KEY_PREFIX + userId);
        return value == null ? 0L : Long.parseLong(value);
    }

    /**
     * 성공적으로 코드를 찾으면 카운터를 지운다.
     */
    public void clearFailedAttempts(Long userId) {
        stringRedisTemplate.delete(ATTEMPT_KEY_PREFIX + userId);
    }

    /**
     * 초대 코드와 그에 해당하는 근무지 ID를 삭제하는 메서드
     * @param inviteCode 삭제할 6자리 초대 코드
     * @param workplaceId 초대 코드에 해당하는 근무지 ID
     */
    public void delete(String inviteCode, Long workplaceId) {
        String inviteCodeKey = INVITE_CODE_KEY_PREFIX + inviteCode;
        String workplaceIdKey = WORKPLACE_ID_KEY_PREFIX + workplaceId;
        stringRedisTemplate.delete(List.of(inviteCodeKey, workplaceIdKey));
    }
}
