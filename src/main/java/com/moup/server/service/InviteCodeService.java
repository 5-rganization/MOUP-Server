package com.moup.server.service;

import com.moup.server.exception.WorkplaceNotFoundException;
import com.moup.server.repository.InviteCodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InviteCodeService {
    private final InviteCodeRepository inviteCodeRepository;
    private RandomStringGenerator inviteCodeGenerator;

    @PostConstruct
    public void init() {
        // 0, O, 1, I를 제외한 숫자와 대문자 알파벳 조합
        String baseCharacters = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        this.inviteCodeGenerator = new RandomStringGenerator.Builder()
                .selectFrom(baseCharacters.toCharArray())
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
}
