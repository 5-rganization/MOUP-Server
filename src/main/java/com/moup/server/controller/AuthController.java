package com.moup.server.controller;

import com.moup.server.exception.DuplicateUserException;
import com.moup.server.exception.ErrorCode;
import com.moup.server.exception.UserNotFoundException;
import com.moup.server.model.dto.LoginRequest;
import com.moup.server.model.dto.RegisterRequest;
import com.moup.server.model.entity.User;
import com.moup.server.service.AuthService;
import com.moup.server.service.UserService;
import com.moup.server.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author neoskyclad
 *
 * 유저 토큰 관리를 위한 Controller
 * <p>- 로그인</p>
 * <p>- 회원가입</p>
 */
@Tag(name = "Auth-Controller", description = "유저 토큰 관리 API 엔드포인트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "소셜 로그인 타입과 아이디를 입력 받아서 로그인")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저"),
            @ApiResponse(responseCode = "500", description = "서버 오류"),
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "로그인을 위한 요청 데이터",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequest.class)
            )
    )
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String idToken = loginRequest.getIdToken();
        // 토큰 유효성 검증
        if (!authService.validateToken(idToken)) {
            
        }
        
        // 토큰 파싱 후 사용자 확인
//        User user = userService.findByProviderId(loginRequest.getProviderId());
        
        // 1. 없으면 회원가입 -> 자동으로
        
        // 2. 있으면 그대로 로그인

        // Access Token 반환
        String token = jwtUtil.createToken(user);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        return ResponseEntity.ok()
                .headers(headers)
                .body(Map.of("userId", user.getId()));
    }

    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "소셜 로그인 정보, 닉네임, 역할을 받아서 회원가입")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "409", description = "중복된 유저"),
            @ApiResponse(responseCode = "500", description = "서버 오류"),
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "회원가입을 위한 요청 데이터",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RegisterRequest.class)
            )
    )
    public ResponseEntity<?> createUser(@RequestBody RegisterRequest registerRequest) {
        userService.createUser(registerRequest);

        LoginRequest loginRequest = LoginRequest.builder().
                provider(registerRequest.getProvider())
                .idToken(registerRequest.getIdToken())
                .build();

        return login(loginRequest);
    }
}
