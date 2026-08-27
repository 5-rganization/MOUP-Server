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

import java.security.SecureRandom;
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

    /**
     * 근무지의 초대 코드를 생성하거나, 이미 존재하면 기존 코드를 반환하는 메서드
     * @param workplaceId 초대 코드를 생성할 근무지 ID
     * @return 6자리 초대 코드
     */
    public String generateInviteCode(Long workplaceId, boolean forceGenerate) {
        // 1. 먼저 해당 근무지에 이미 유효한 초대 코드가 있는지 확인합니다.
        Optional<String> existingInviteCode = inviteCodeRepository.findInviteCodeByWorkplaceId(workplaceId);
        if (existingInviteCode.isPresent()) {
            if (forceGenerate) {
                // 1-1. 초대 코드 재생성이 요청된 경우, 해당 초대 코드를 삭제합니다.
                inviteCodeRepository.delete(existingInviteCode.get(), workplaceId);
            } else {
                // 1-2. 초대 코드 재생성이 요청되지 않은 경우, 기존 코드를 반환합니다.
                return existingInviteCode.get();
            }
        }

        // 2. 기존 코드가 없거나 초대 코드 재생성이 요청된 경우, 새로운 코드 생성을 시도합니다.
        int maxAttempts = 10;  // 중복되지 않는 코드를 찾기 위한 최대 시도 횟수
        for (int i = 0; i < maxAttempts; i++) {
            String inviteCode = inviteCodeGenerator.generate(6);

            // 생성된 코드가 이미 사용 중인지 확인합니다.
            if (!inviteCodeRepository.existsByInviteCode(inviteCode)) {
                inviteCodeRepository.save(inviteCode, workplaceId);
                return inviteCode;
            }
        }

        // 최대 시도 횟수를 초과하면 예외를 발생시킵니다.
        throw new RuntimeException("초대 코드 생성에 실패하였습니다. 잠시 후 다시 시도해주세요.");
    }

    /**
     * 근무지 ID로 초대 코드를 찾는 메서드
     * @param workplaceId 조회할 근무지 ID
     * @return 초대 코드(문자열)를 포함한 Optional 객체
     */
    public boolean existsByWorkplaceId(Long workplaceId) {
        return inviteCodeRepository.existsByWorkplaceId(workplaceId);
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
