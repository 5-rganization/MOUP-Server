package com.moup.server.util;

import com.moup.server.exception.InvalidFieldFormatException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class NameVerifyUtil {
    private static final Pattern INCOMPLETE_HANGUL_PATTERN = Pattern.compile("[ㄱ-ㅎㅏ-ㅣ]");
    private static final Pattern HANGUL_PATTERN = Pattern.compile("[가-힣]");
    private static final Pattern ALPHABET_PATTERN = Pattern.compile("[a-zA-Z]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^가-힣a-zA-Z0-9]");

    /**
     * 공백 허용 불가
     * 자음만 단독 사용 불가
     * 모음만 단독 사용 불가
     * 정확한 단어만 사용 가능 예) ㄱㅏ (X)
     * 한글, 영문 혼합 불가
     * 특수문자 사용 불가
     * 8자 이하 사용 가능
     *
     * @param name
     * @return
     */
    public boolean verifyName(String name) {
        // 이름이 null이거나 비어있는지 확인
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // Regex 설명:
        // ^[가-힣]{1,8}$ : 1~8자의 완성된 한글 문자만 허용합니다. (자음/모음 단독 사용 불가)
        // ^[a-zA-Z]{1,8}$ : 1~8자의 영문 대소문자만 허용합니다.
        String koreanRegex = "^[가-힣]{1,8}$";
        String englishRegex = "^[a-zA-Z]{1,8}$";

        // 이름이 한글 정규식 또는 영문 정규식 중 하나와 일치하는지 확인합니다.
        return name.matches(koreanRegex) || name.matches(englishRegex);
    }

    /// 닉네임의 유효성을 검사하고, 규칙에 어긋나면 {@link InvalidFieldFormatException}을 발생시킵니다.
    ///
    /// ## 닉네임 허용 규칙
    /// - **길이**: 1자 이상 8자 이하
    /// - **문자**: 한글, 영문, 숫자만 사용 가능
    ///
    /// ## 닉네임 제한 규칙
    /// - **혼용 불가**: 한글과 영문은 함께 사용할 수 없음
    /// - **공백 불가**: 닉네임의 시작, 끝, 중간에 공백 사용 불가
    /// - **특수문자 불가**: 모든 특수문자 사용 불가
    /// - **불완전한 한글 불가**:
    /// - 'ㄱ', 'ㄴ' 등 자음만으로 구성 불가
    /// - 'ㅏ', 'ㅑ' 등 모음만으로 구성 불가
    /// - 'ㄱㅏ', 'ㄷㅐ' 등 조합이 완성되지 않은 글자 사용 불가
    ///
    /// @param nickname 검사할 닉네임 문자열
    /// @throws IllegalArgumentException 닉네임이 유효성 규칙에 맞지 않을 경우
    ///
    public void verifyNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new InvalidFieldFormatException("닉네임에 빈 값이 들어올 수 없습니다.");
        }

        if (nickname.contains(" ")) {
            throw new InvalidFieldFormatException("닉네임 앞뒤 또는 중간에 공백을 사용할 수 없습니다.");
        }

        String trimmed = nickname.trim();

        if (INCOMPLETE_HANGUL_PATTERN.matcher(trimmed).find()) {
            throw new InvalidFieldFormatException("정확한 글자를 입력해야 합니다.");
        }

        boolean containsHangul = HANGUL_PATTERN.matcher(trimmed).find();
        boolean containsAlphabet = ALPHABET_PATTERN.matcher(trimmed).find();
        if (containsHangul && containsAlphabet) {
            throw new InvalidFieldFormatException("한글 또는 영문만 사용할 수 있습니다.");
        }

        if (SPECIAL_CHAR_PATTERN.matcher(trimmed).find()) {
            throw new InvalidFieldFormatException("특수문자는 사용할 수 없습니다.");
        }

        if (trimmed.length() > 8) {
            throw new InvalidFieldFormatException("8자 이하로 입력해야 합니다.");
        }
    }
}
