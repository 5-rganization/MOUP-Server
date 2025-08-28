package com.moup.server.util;

import org.springframework.stereotype.Component;

import javax.naming.InvalidNameException;

@Component
public class NameVerifyUtil {

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
}
