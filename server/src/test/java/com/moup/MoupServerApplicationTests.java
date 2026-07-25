package com.moup;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.moup.domain.auth.application.AppleAuthService;
import com.moup.global.util.AppleJwtUtil;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class MoupServerApplicationTests {

    @MockitoBean
    private AppleAuthService appleAuthService;

    @MockitoBean
    private AppleJwtUtil appleJwtUtil;

    @Test
    void contextLoads() {

    }
}
