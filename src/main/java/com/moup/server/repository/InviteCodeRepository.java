package com.moup.server.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class InviteCodeRepository {
    private static final String INVITE_CODE_KEY_PREFIX = "inviteCode:";
    private static final String WORKPLACE_ID_KEY_PREFIX = "workplaceId:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 초대 코드와 근무지 ID를 10분간 Redis에 양방향으로 저장하는 메서드
     * @param inviteCode 저장할 6자리 초대 코드
     * @param workplaceId 초대 코드에 해당하는 근무지 ID
     */
    public void save(String inviteCode, Long workplaceId) {
        String inviteCodeKey = INVITE_CODE_KEY_PREFIX + inviteCode;
        String workplaceIdKey = WORKPLACE_ID_KEY_PREFIX + workplaceId;

        // inviteCode -> workplaceId 저장
        stringRedisTemplate.opsForValue().set(inviteCodeKey, workplaceId.toString(), 10, TimeUnit.MINUTES);
        // workplaceId -> inviteCode 저장 (역방향 매핑)
        stringRedisTemplate.opsForValue().set(workplaceIdKey, inviteCode, 10, TimeUnit.MINUTES);
    }

    /**
     * 해당 초대 코드가 Redis에 존재하는지 확인하는 메서드
     * @param inviteCode 확인할 6자리 초대 코드
     * @return 존재하면 true, 그렇지 않으면 false
     */
    public boolean exists(String inviteCode) {
        String key = INVITE_CODE_KEY_PREFIX + inviteCode;
        return stringRedisTemplate.hasKey(key);
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
    public Optional<String> findWorkplaceIdByInviteCode(String inviteCode) {
        String key = INVITE_CODE_KEY_PREFIX + inviteCode;
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(key));
    }
}