package com.moup.server;

import com.moup.server.common.Login;
import com.moup.server.model.entity.User;
import com.moup.server.service.AuthService;
import com.moup.server.service.AuthServiceFactory;
import com.moup.server.service.UserService;
import com.moup.server.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
class MoupServerApplicationTests {

    @Autowired
    private MockMvc mockMvc; // Controller 테스트를 위한 MockMvc

    @MockitoBean
    private AuthServiceFactory authServiceFactory; // AuthServiceFactory를 모의 객체로 대체

    @MockitoBean
    private UserService userService; // UserService를 모의 객체로 대체

    @MockitoBean
    private JwtUtil jwtUtil; // JwtUtil을 모의 객체로 대체

    @MockitoBean
    private AuthService googleAuthService; // 특정 AuthService 구현체도 모의 가능


    @Test
    void login_기존유저_로그인_성공() throws Exception {
        // given
        String testAuthCode = "test-auth-code";
        Login provider = Login.LOGIN_GOOGLE;
        String providerId = "google-user-123";
        User mockUser = new User(); // 테스트용 User 객체 생성
        String testJwt = "mock-jwt-token";

        // 1. Mock 객체의 동작 정의
        // Auth Code 교환 시 유저 정보 반환
        Map<String, Object> mockUserInfo = new HashMap<>();
        mockUserInfo.put("userId", providerId);
        mockUserInfo.put("name", "테스트유저");
        Mockito.when(googleAuthService.exchangeAuthCode(testAuthCode)).thenReturn(mockUserInfo);
        Mockito.when(googleAuthService.getProviderId(mockUserInfo)).thenReturn(providerId);

        // AuthServiceFactory가 특정 제공자에 대해 mock 객체를 반환하도록 설정
        Mockito.when(authServiceFactory.getService(provider)).thenReturn(googleAuthService);

        // 유저 조회 시 mockUser 반환
        Mockito.when(userService.findByProviderAndId(provider, providerId)).thenReturn(mockUser);

        // JWT 생성 시 mockJwt 반환
        Mockito.when(jwtUtil.createAccessToken(mockUser)).thenReturn(testJwt);

        // 2. MockMvc를 사용하여 컨트롤러 메서드 호출
        String requestBody = "{\"provider\": \"" + provider.name() + "\", \"authCode\": \"" + testAuthCode + "\"}";
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().string("Authorization", "Bearer " + testJwt));
    }

}
