package com.moup.domain.workplace.mapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

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

    /**
     * 초대 코드와 그에 해당하는 근무지 ID를 10분간 Redis에 양방향으로 저장하는 메서드
     * @param inviteCode 저장할 6자리 초대 코드
     * @param workplaceId 초대 코드에 해당하는 근무지 ID
     */
    public void save(String inviteCode, Long workplaceId) {
        String inviteCodeKey = INVITE_CODE_KEY_PREFIX + inviteCode;
        String workplaceIdKey = WORKPLACE_ID_KEY_PREFIX + workplaceId;

        stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings({"unchecked"})  // 경고를 무시하기 위한 어노테이션
            public List<Object> execute(@NonNull RedisOperations operations) throws DataAccessException {
                // 1. 트랜잭션 시작
                operations.multi();

                // 2. 명령어들을 큐에 쌓음
                // inviteCode -> workplaceId 저장
                operations.opsForValue().set(inviteCodeKey, workplaceId.toString(), 10, TimeUnit.MINUTES);
                // workplaceId -> inviteCode 저장 (역방향 매핑)
                operations.opsForValue().set(workplaceIdKey, inviteCode, 10, TimeUnit.MINUTES);

                // 3. 모든 명령어를 원자적으로 실행
                return operations.exec();
            }
        });
    }

    /**
     * 해당 초대 코드가 Redis에 존재하는지 확인하는 메서드
     * @param inviteCode 확인할 6자리 초대 코드
     * @return 존재하면 true, 그렇지 않으면 false
     */
    public boolean existsByInviteCode(String inviteCode) {
        String key = INVITE_CODE_KEY_PREFIX + inviteCode;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 해당 근무지 ID가 Redis에 존재하는지 확인하는 메서드
     * @param workplaceId 확인할 근무지 ID
     * @return 존재하면 true, 그렇지 않으면 false
     */
    public boolean existsByWorkplaceId(Long workplaceId) {
        String key = WORKPLACE_ID_KEY_PREFIX + workplaceId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
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
