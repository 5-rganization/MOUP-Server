package com.moup.server.service;

import com.moup.server.model.entity.SocialToken;
import com.moup.server.repository.SocialTokenRepository;
import jakarta.security.auth.message.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseAuthService implements AuthService {

    protected final SocialTokenRepository socialTokenRepository;

    // `getProviderId`와 `getUsername`은 모든 서비스에서 동일하게 작동하므로 공통 구현을 제공합니다.
    @Override
    public String getProviderId(Map<String, Object> userInfo) { return (String) userInfo.get("userId"); }

    // Apple은 이름 정보를 제공하지 않으므로 null을 반환할 수 있습니다.
    @Override
    public String getUsername(Map<String, Object> userInfo) { return (String) userInfo.get("name"); }

    /// \[템플릿 메서드\] 토큰 해제의 전체적인 흐름을 정의합니다.
    @Override
    @Retryable(
            retryFor = { IOException.class },
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void revokeToken(Long userId) throws AuthException, IOException {
        // 1. DB에서 공급자(provider)에 맞는 리프레시 토큰 조회
        SocialToken socialToken = socialTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthException(getProvider().toString() + " 소셜 리프레시 토큰이 없습니다."));

        // 2. 각 공급자(provider)에 맞는 요청 본문 생성
        String postData = buildRevokeRequestBody(socialToken.getRefreshToken());

        // 3. 공통 HTTP 요청 로직 호출
        sendPostRequest(new URL(getRevokeUrl()), postData);
    }

    /// 모든 재시도가 실패했을 때 호출될 복구 메서드
    @Recover
    public void recoverRevokeToken(IOException e, Long userId) throws AuthException {
        log.error("All retries failed for revokeToken. userId: {}. Error: {}", userId, e.getMessage());
        throw new AuthException(getProvider().toString() + " Revoke API 호출 중 오류가 발생했습니다.", e);
        // 여기에 실패 내역을 DB에 기록하는 등의 후처리 로직을 추가할 수 있습니다.
    }

    /// \[헬퍼 메서드\] POST 요청을 보내고 응답 코드가 200 OK가 아니면 예외를 던집니다.
    /// `revokeToken`에서 사용됩니다.
    protected void sendPostRequest(URL url, String postData) throws IOException, AuthException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = postData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                throw new AuthException(getProvider().toString() + " API 호출 실패 (" + responseCode + "): " + response);
            }
        }
    }

    /// \[헬퍼 메서드\] POST 요청을 보내고 응답 본문을 문자열로 반환합니다.
    /// `exchangeAuthCode`처럼 응답을 파싱해야 할 때 사용됩니다.
    protected String sendPostRequestAndGetResponse(URL url, String postData) throws IOException, AuthException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = postData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        // 응답을 읽을 스트림을 결정합니다. (성공 시 InputStream, 실패 시 ErrorStream)
        InputStreamReader reader = new InputStreamReader(
                responseCode == HttpURLConnection.HTTP_OK ? connection.getInputStream() : connection.getErrorStream()
        );

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new AuthException(getProvider().toString() + " API 호출 실패 (" + responseCode + "): " + response);
        }

        return response.toString();
    }

    // --- 하위 클래스에서 반드시 구현해야 할 추상 메서드들 ---

    /// 토큰 해제 API의 전체 URL
    protected abstract String getRevokeUrl();

    /// 토큰 해제 API 요청에 필요한 본문(body) 데이터를 생성
    protected abstract String buildRevokeRequestBody(String refreshToken) throws AuthException;
}
