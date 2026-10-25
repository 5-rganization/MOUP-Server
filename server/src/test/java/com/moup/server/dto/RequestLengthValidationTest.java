package com.moup.server.dto;

import com.moup.domain.user.dto.OwnerWorkplaceCreateRequest;
import com.moup.domain.work.dto.MyWorkCreateRequest;
import com.moup.domain.workplace.domain.WorkplaceCategory;
import com.moup.global.util.TextUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/// 스키마 길이·범위를 넘는 입력이 DB에서 500이 되지 않고 요청 검증에서 422로 걸리는지 (스코프 4 I9 · 5 I-3).
class RequestLengthValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> violatedFields(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath).map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("근무지 생성: 이름 51자·주소 101자·위도 범위 밖·라벨색 11자를 거부한다")
    void 근무지_생성_검증() {
        OwnerWorkplaceCreateRequest request = OwnerWorkplaceCreateRequest.builder()
                .workplaceName("가".repeat(51)).categoryName(WorkplaceCategory.CVS)
                .address("가".repeat(101)).latitude(99999.0).longitude(127.0)
                .ownerBasedLabelColor("X".repeat(11)).build();

        assertEquals(Set.of("workplaceName", "address", "latitude", "ownerBasedLabelColor"), violatedFields(request));
    }

    @Test
    @DisplayName("근무 등록: 메모 201자를 거부한다")
    void 메모_길이() {
        MyWorkCreateRequest request = MyWorkCreateRequest.builder()
                .routineIdList(List.of()).repeatDays(List.of()).restTimeMinutes(0)
                .startTime(Instant.now()).endTime(Instant.now().plusSeconds(3600))
                .memo("가".repeat(201)).build();

        assertEquals(Set.of("memo"), violatedFields(request));
    }

    @Test
    @DisplayName("소셜 이름은 20자(코드 포인트)로 자르고, 이모지를 반으로 가르지 않는다")
    void 이름_자르기() {
        assertEquals("가".repeat(20), TextUtil.truncateCodePoints("가".repeat(25), 20));
        String emojis = "😀".repeat(21);   // UTF-16으로 42칸, 문자로 21자
        assertEquals("😀".repeat(20), TextUtil.truncateCodePoints(emojis, 20));
        assertEquals("짧은 이름", TextUtil.truncateCodePoints("짧은 이름", 20));
        assertNull(TextUtil.truncateCodePoints(null, 20));
    }

    /// 발급 알파벳에 없는 0·1·O·I가 든 코드는 404가 아니라 형식 오류로 바로 알려 준다(스코프 5 M-9).
    @Test
    @DisplayName("초대 코드 형식은 발급 알파벳과 같다 — 0·1·O·I는 형식 오류, 소문자는 허용")
    void 초대_코드_형식() {
        java.util.function.Function<String, Set<String>> check = code -> violatedFields(
                com.moup.domain.workplace.dto.WorkplaceJoinRequest.builder().inviteCode(code).workerBasedLabelColor("RED")
                        .salaryCreateRequest(com.moup.domain.salary.dto.SalaryCreateRequest.builder()
                                .salaryType(com.moup.domain.salary.domain.SalaryType.SALARY_MONTHLY)
                                .salaryCalculation(com.moup.domain.salary.domain.SalaryCalculation.SALARY_CALCULATION_HOURLY)
                                .hourlyRate(10_320).hasNationalPension(false).hasHealthInsurance(false)
                                .hasEmploymentInsurance(false).hasIndustrialAccident(false).hasIncomeTax(false)
                                .hasHolidayAllowance(false).hasNightAllowance(false).build())
                        .build());

        assertEquals(Set.of(), check.apply("MUP234"));
        assertEquals(Set.of(), check.apply("mupze4"));
        for (String bad : List.of("MUP0E4", "MUP1E4", "MUPOE4", "MUPIE4", "MUP23")) {
            assertEquals(Set.of("inviteCode"), check.apply(bad), bad);
        }
    }
}
