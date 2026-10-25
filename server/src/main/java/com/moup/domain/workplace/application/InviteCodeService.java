package com.moup.domain.workplace.application;

import com.moup.domain.workplace.exception.WorkplaceNotFoundException;
import com.moup.domain.workplace.mapper.InviteCodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.RandomStringGenerator;
import com.moup.global.error.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.moup.domain.workplace.exception.InviteCodeGenerationException;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InviteCodeService {
    private final InviteCodeRepository inviteCodeRepository;

    @Value("${workplace.invite-code.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${workplace.invite-code.attempt-window-minutes}")
    private int attemptWindowMinutes;

    /// 보안 파라미터라 설정으로 뺐다. 예전에는 저장소 코드에 10분이 박혀 있었다(스코프 5 M-3).
    @Value("${workplace.invite-code.ttl-minutes:10}")
    private int inviteCodeTtlMinutes;
    private RandomStringGenerator inviteCodeGenerator;

    /// 초대코드는 근무지 참여 자격이므로 예측 불가능해야 한다.
    /// `RandomStringGenerator`는 `usingRandom`을 주지 않으면 `ThreadLocalRandom`(비 CSPRNG)으로
    /// 폴백한다 — commons-text 1.14.0 바이트코드에서 확인.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @PostConstruct
    public void init() {
        // 0, O, 1, I를 제외한 숫자와 대문자 알파벳 조합
        String baseCharacters = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        this.inviteCodeGenerator = new RandomStringGenerator.Builder()
                .selectFrom(baseCharacters.toCharArray())
                .usingRandom(SECURE_RANDOM::nextInt)
                .get();
    }

    /// 발급 결과. `reused`면 새로 만들지 않고 기존 코드를 돌려준 것이다.
    public record IssuedInviteCode(String code, boolean reused) {}

    /// 근무지의 초대 코드를 돌려준다. 유효한 코드가 있으면 그대로, 없거나 `forceGenerate`면 새로 만든다.
    ///
    /// 기존 코드 여부를 결과에 담는다. 예전에는 호출자가 `existsByWorkplaceId`로 따로 물은 뒤 이 메서드를 불러
    /// Redis를 두 번 왕복했고, 그 사이 TTL이 끝나면 새 코드인데 "기존 코드"라고 답했다(스코프 5 M-2).
    public IssuedInviteCode generateInviteCode(Long workplaceId, boolean forceGenerate) {
        Optional<String> existingInviteCode = inviteCodeRepository.findInviteCodeByWorkplaceId(workplaceId);
        if (existingInviteCode.isPresent()) {
            if (!forceGenerate) {
                return new IssuedInviteCode(existingInviteCode.get(), true);
            }
            inviteCodeRepository.delete(existingInviteCode.get(), workplaceId);
        }

        // 32자 알파벳 6자리 = 약 10억 가지. 충돌은 드물지만, 나면 다른 근무지의 코드를 덮어쓰지 않고 다시 뽑는다.
        Duration ttl = Duration.ofMinutes(inviteCodeTtlMinutes);
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            String inviteCode = inviteCodeGenerator.generate(6);
            if (inviteCodeRepository.claim(inviteCode, workplaceId, ttl)) {
                return new IssuedInviteCode(inviteCode, false);
            }
        }

        // 예전에는 RuntimeException이라 사용자에게 "서버에 오류가 발생했습니다."만 갔다(스코프 5 M-1).
        throw new InviteCodeGenerationException();
    }

    /// 근무지가 삭제되면 그 코드도 지운다. 예전에는 최대 TTL 동안 Redis에 남았다(스코프 5 M-5).
    public void deleteByWorkplaceId(Long workplaceId) {
        inviteCodeRepository.findInviteCodeByWorkplaceId(workplaceId)
                .ifPresent(code -> inviteCodeRepository.delete(code, workplaceId));
    }

    /**
     * 초대 코드로 근무지 ID를 찾는 메서드
     * @param inviteCode 조회할 6자리 초대 코드
     * @return 근무지 ID(Long)를 포함한 Optional 객체
     */
    public Long findWorkplaceIdByInviteCode(String inviteCode) {
        return inviteCodeRepository.findWorkplaceIdByInviteCode(inviteCode).orElseThrow(WorkplaceNotFoundException::new);
    }

    /// 초대코드 조회를 레이트 리밋과 함께 수행한다.
    ///
    /// **표적 공격은 원래 안전하다** — 키스페이스 32⁶ ≈ 10.7억, TTL 10분이라
    /// 특정 코드를 맞히려면 초당 100만 회가 필요하다.
    ///
    /// **문제는 무표적 공격이다.** 아무 코드나 맞히면 되므로 동시 유효 코드가 L개일 때
    /// 기댓값이 2³⁰/L회로 줄어든다. 초당 2,000회 기준 L=500이면 약 18분,
    /// L=5,000이면 약 2분이다. **제품이 성장할수록 나빠진다.**
    /// 뚫리면 근무지 이름·주소·GPS가 새고, 200/404 응답 자체가 스크래핑 오라클이 된다.
    ///
    /// 실패만 세는 이유: 정상 사용자는 코드를 한두 번 잘못 입력할 뿐이고,
    /// 무차별 대입은 정의상 실패가 압도적이다.
    /// IP 단위 제한은 여기가 아니라 nginx/ALB에 두는 것이 맞다.
    public Long findWorkplaceIdByInviteCodeWithRateLimit(Long userId, String inviteCode) {
        if (inviteCodeRepository.countFailedAttempts(userId) >= maxFailedAttempts) {
            log.warn("초대코드 조회 제한 초과 - userId={}", userId);
            throw new TooManyRequestsException(
                    "초대 코드 조회 시도가 너무 많습니다. " + attemptWindowMinutes + "분 후 다시 시도해주세요.");
        }
        try {
            Long workplaceId = findWorkplaceIdByInviteCode(inviteCode);
            inviteCodeRepository.clearFailedAttempts(userId);
            return workplaceId;
        } catch (WorkplaceNotFoundException e) {
            long attempts = inviteCodeRepository.recordFailedAttempt(userId, attemptWindowMinutes);
            log.info("초대코드 조회 실패 - userId={}, 누적={}", userId, attempts);
            throw e;
        }
    }
}
