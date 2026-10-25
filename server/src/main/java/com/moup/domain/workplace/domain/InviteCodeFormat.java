package com.moup.domain.workplace.domain;

/// 초대 코드 형식. 발급 알파벳(`InviteCodeService`)과 같아야 한다.
///
/// 발급은 헷갈리는 0·O·1·I를 빼는데 검증은 영숫자 전체를 받아, `MUP2E4`를 `MUPZE4`로 잘못 읽거나 O를 0으로 친
/// 코드가 안내 없이 "근무지 없음"(404)이 되고 레이트 리밋 실패 횟수에도 쌓였다(스코프 5 M-9).
/// 이제 쓰일 수 없는 글자는 형식 오류(422)로 바로 알려 준다. 소문자는 서버가 대문자로 바꾸므로 받는다.
public final class InviteCodeFormat {

    private InviteCodeFormat() {
    }

    public static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    public static final String REGEX = "^[2-9A-HJ-NP-Za-hj-np-z]{6}$";
    public static final String MESSAGE = "초대 코드는 6자리이며 숫자 0·1과 영문 O·I는 쓰이지 않습니다.";
}
