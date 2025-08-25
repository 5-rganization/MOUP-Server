package com.moup.server.controller;

import com.moup.server.common.Login;
import com.moup.server.common.Role;
import com.moup.server.exception.InvalidTokenException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.service.*;
import com.moup.server.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.security.auth.message.AuthException;
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
    private final SocialTokenService socialTokenService;
    private final UserTokenService userTokenService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "소셜 로그인 타입과 토큰을 입력 받아서 로그인")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))), @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))), @ApiResponse(responseCode = "409", description = "삭제 처리된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))), @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "로그인을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginRequest.class)))
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) throws AuthException {
        Login provider = loginRequest.getProvider();
        String authCode = loginRequest.getAuthCode();

        // Factory에서 주입 받아서 공통 로직 수행 -> OCP 지키기
        AuthService service = authServiceFactory.getService(provider);

        // 1. Auth Code로 유저 정보 교환
        Map<String, Object> userInfo = service.exchangeAuthCode(authCode);
        String providerId = service.getProviderId(userInfo);

        // 2. 유저 정보로 DB에서 가입 여부 확인
        User user = userService.findByProviderAndId(provider, providerId);  // 없을 시 404 반환 후 GlobalExceptionHandler가 처리

        // 3. 토큰 관리
        // 3-1. 소셜 토큰 관리
        String socialRefreshToken = (String) userInfo.get("socialRefreshToken");
        if (!socialRefreshToken.isEmpty()) {
            socialTokenService.saveOrUpdateToken(socialRefreshToken);
        }

        // 3-2. 서비스 토큰 관리
        String accessToken = jwtUtil.createAccessToken(user);
        String refreshToken = jwtUtil.createRefreshToken(user);
        userTokenService.saveOrUpdateToken(refreshToken, jwtUtil.getRefreshTokenExpiration());

        LoginResponse loginResponse = LoginResponse.builder().userId(user.getId()).role(user.getRole()).accessToken(accessToken).refreshToken(refreshToken).build();
        return ResponseEntity.ok().body(loginResponse);
    }

    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "소셜 로그인 정보, 닉네임, 역할을 받아서 회원가입")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "회원가입 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterResponse.class))), @ApiResponse(responseCode = "409", description = "중복된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))), @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "회원가입을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterRequest.class)))
    public ResponseEntity<?> createUser(@RequestBody RegisterRequest registerRequest) throws AuthException {
        Login provider = registerRequest.getProvider();
        String authCode = registerRequest.getAuthCode();

        // Factory에서 주입 받아서 공통 로직 수행 -> OCP 지키기
        AuthService service = authServiceFactory.getService(provider);

        // 1. Auth Code로 유저 정보 교환
        Map<String, Object> userInfo = service.exchangeAuthCode(authCode);
        String providerId = service.getProviderId(userInfo);

        // 2. DB 저장을 위한 User 엔티티 생성
        String username = service.getUsername(userInfo);
        if (username == null) {
            // Apple 로그인의 경우 클라이언트를 통해 유저 이름 수신
            username = registerRequest.getUsername();
        }

        User user = User.builder().provider(provider).providerId(providerId).username(username).nickname(registerRequest.getNickname()).role(Role.valueOf(registerRequest.getRole())).build();

        userService.createUser(user);

        // 3. 토큰 관리
        // 3-1. 소셜 토큰 관리
        String socialRefreshToken = (String) userInfo.get("socialRefreshToken");
        if (!socialRefreshToken.isEmpty()) {
            // Apple 로그인의 경우 Revoke를 위한 Social Refresh Token 저장
            socialTokenService.saveOrUpdateToken(socialRefreshToken);
        }

        // 3-2. 우리 서비스 토큰 관리
        String accessToken = jwtUtil.createAccessToken(user);
        String refreshToken = jwtUtil.createRefreshToken(user);
        userTokenService.saveOrUpdateToken(refreshToken, jwtUtil.getRefreshTokenExpiration());

        RegisterResponse registerResponse = RegisterResponse.builder().userId(providerId).role(user.getRole()).accessToken(accessToken).refreshToken(refreshToken).build();
        return ResponseEntity.ok().body(registerResponse);
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰으로 액세스 토큰 재발급")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "재발급 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RefreshTokenResponse.class))), @ApiResponse(responseCode = "400", description = "유효하지 않은 토큰", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))), @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))), @ApiResponse(responseCode = "409", description = "삭제 처리된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))), @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "액세스 토큰 재발급을 위한 요청 DTO", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RefreshTokenRequest.class)))
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        if (!userTokenService.isValidRefreshToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        // 액세스 토큰 반환
        Long userId = jwtUtil.getUserId(refreshToken);

        User user = userService.findUserById(userId);
        String accessToken = jwtUtil.createAccessToken(user);
        String newRefreshToken = jwtUtil.createRefreshToken(user);
        userTokenService.saveOrUpdateToken(newRefreshToken, jwtUtil.getRefreshTokenExpiration());

        RefreshTokenResponse refreshTokenResponse = RefreshTokenResponse.builder().accessToken(accessToken).refreshToken(newRefreshToken).build();

        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").header(HttpHeaders.PRAGMA, "no-cache").body(refreshTokenResponse);
    }
}
