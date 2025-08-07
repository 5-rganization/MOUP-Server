package com.moup.server.controller;

import com.moup.server.common.Login;
import com.moup.server.common.Role;
import com.moup.server.model.dto.ErrorResponse;
import com.moup.server.model.dto.LoginRequest;
import com.moup.server.model.dto.LoginResponse;
import com.moup.server.model.dto.RegisterRequest;
import com.moup.server.model.dto.RegisterResponse;
import com.moup.server.model.entity.User;
import com.moup.server.service.AuthService;
import com.moup.server.service.AuthServiceFactory;
import com.moup.server.service.UserService;
import com.moup.server.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author neoskyclad
 * <p>
 * 유저 토큰 관리를 위한 Controller
 * <p>- 로그인</p>
 * <p>- 회원가입</p>
 */
@Tag(name = "Auth-Controller", description = "유저 토큰 관리 API 엔드포인트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthServiceFactory authServiceFactory;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "소셜 로그인 타입과 아이디를 입력 받아서 로그인")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "로그인을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginRequest.class)))
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Login provider = loginRequest.getProvider();
        String idToken = loginRequest.getIdToken();
        String providerId = "";

        try {
            // Factory에서 주입 받아서 공통 로직 수행 -> OCP 지키기
            AuthService service = authServiceFactory.getService(provider);

            Map<String, Object> userInfo = service.verifyIdToken(idToken);
            providerId = userInfo.get("userId").toString();

        } catch (GeneralSecurityException | IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        // 토큰 파싱 후 사용자 확인
        User user = userService.findByProviderAndId(provider, providerId);

        // Access Token 반환
        String token = jwtUtil.createToken(user);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        LoginResponse loginResponse = LoginResponse.builder().userId(providerId).build();

        return ResponseEntity.ok().headers(headers).body(loginResponse);
    }

    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "소셜 로그인 정보, 닉네임, 역할을 받아서 회원가입")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "409", description = "중복된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "회원가입을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterRequest.class)))
    public ResponseEntity<?> createUser(@RequestBody RegisterRequest registerRequest) {
        Login provider = registerRequest.getProvider();
        String idToken = registerRequest.getIdToken();
        String providerId = "";
        String username = "moup";

        try {
            // Factory에서 주입 받아서 공통 로직 수행 -> OCP 지키기
            AuthService service = authServiceFactory.getService(provider);

            Map<String, Object> userInfo = service.verifyIdToken(idToken);
            providerId = userInfo.get("userId").toString();
            username = userInfo.get("name").toString();

        } catch (GeneralSecurityException | IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        User user = User.builder().provider(provider).providerId(providerId).username(username)
                .nickname(registerRequest.getNickname()).role(Role.valueOf(registerRequest.getRole())).build();

        userService.createUser(user);

        String token = jwtUtil.createToken(user);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        RegisterResponse registerResponse = RegisterResponse.builder().userId(providerId).role(user.getRole()).build();

        return ResponseEntity.ok().headers(headers).body(registerResponse);
    }
}
