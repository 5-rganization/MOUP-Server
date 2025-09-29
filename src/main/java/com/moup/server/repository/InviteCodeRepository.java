package com.moup.server.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class InviteCodeRepository {
    private static final String KEY_PREFIX = "inviteCode: ";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 초대 코드를 10분간 Redis에 저장하는 메서드
     * @param inviteCode 저장할 6자리 초대 코드
     */
    public void save(String inviteCode) {
        String key = KEY_PREFIX + inviteCode;
        stringRedisTemplate.opsForValue().set(key, "valid", 10, TimeUnit.MINUTES);
    }

    /**
     * 해당 초대 코드가 Redis에 유효하게 존재하는지 확인하는 메서드
     * @param inviteCode 확인할 6자리 초대 코드
     * @return 존재하면 true, 그렇지 않으면 false
     */
    public boolean exists(String inviteCode) {
        String key = KEY_PREFIX + inviteCode;
        return stringRedisTemplate.hasKey(key);
    }
}
