package com.moup.global.util;

public final class TextUtil {

    private TextUtil() {
    }

    /// `maxLength`자(코드 포인트)까지만 남긴다. MySQL VARCHAR 길이는 문자 수라 Java `length()`(UTF-16 단위)로
    /// 자르면 이모지가 반으로 갈리거나, 반대로 이모지가 섞인 값을 너무 짧게 자른다.
    public static String truncateCodePoints(String value, int maxLength) {
        if (value == null || value.codePointCount(0, value.length()) <= maxLength) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxLength));
    }
}
