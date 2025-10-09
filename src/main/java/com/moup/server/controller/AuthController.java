package com.moup.server.controller;

import com.moup.server.common.Login;
import com.moup.server.common.Role;
import com.moup.server.exception.InvalidArgumentException;
import com.moup.server.exception.InvalidTokenException;
import com.moup.server.model.dto.*;
import com.moup.server.model.entity.User;
import com.moup.server.service.*;
import com.moup.server.util.JwtUtil;
import com.moup.server.util.NameVerifyUtil;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.naming.InvalidNameException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

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
    private final IdentityService identityService;
    private final SocialTokenService socialTokenService;
    private final UserTokenService userTokenService;
    private final JwtUtil jwtUtil;
    private final NameVerifyUtil nameVerifyUtil;

    @PostMapping("/login")
    @Operation(summary = "소셜 로그인 혹은 유저 생성(회원가입 절차 시작)", description = "소셜 로그인 타입과 토큰을 입력 받아서 로그인, 토큰이나 회원 정보가 없을 경우 유저 정보 생성 및 회원가입 절차 시작")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class, example =
                    """
                    {
                        "userId": 1,
                        "role": "ROLE_WORKER",
                        "accessToken": "string",
                        "refreshToken": "string"
                    }
                    """))),
            @ApiResponse(responseCode = "201", description = "회원가입 절차 시작 (유저 생성)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class, example =
                    """
                    {
                        "userId": 1,
                        "accessToken": "string",
                        "refreshToken": "string"
                    }
                    """))),
            @ApiResponse(responseCode = "202", description = "회원가입 절차 진행중", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class, example =
                    """
                    {
                        "userId": 1,
                        "accessToken": "string",
                        "refreshToken": "string"
                    }
                    """))),
            @ApiResponse(responseCode = "409", description = "삭제 처리된 유저", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "로그인을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginRequest.class)))
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) throws AuthException, InvalidNameException {
        Login provider = loginRequest.getProvider();
        String authCode = loginRequest.getAuthCode();

        // Factory에서 주입 받아서 공통 로직 수행 -> OCP 지키기
        AuthService service = authServiceFactory.getService(provider);

        // 1. Auth Code로 유저 정보 교환
        Map<String, Object> userInfo = service.exchangeAuthCode(authCode);
        String providerId = service.getProviderId(userInfo);
        String socialRefreshToken = (String) userInfo.get("socialRefreshToken");

        // 2. 유저 정보로 DB에서 가입 여부 확인
        Optional<User> optionalUser = userService.findByProviderAndId(provider, providerId);

        if (optionalUser.isPresent()) {
            // 3-a. 유저 정보가 있으면 로그인
            User user = optionalUser.get();

            // 3-a-1. 탈퇴 처리중인 경우 탈퇴 철회
            if (user.getIsDeleted()) {
                userService.restoreUserByUserId(user.getId());
            }

            // 3-a-2. 소셜 토큰 관리
            if (socialRefreshToken != null && !socialRefreshToken.isEmpty()) {
                socialTokenService.saveOrUpdateToken(user.getId(), socialRefreshToken);
            }
            TokenCreateRequest tokenCreateRequest = TokenCreateRequest.builder().userId(user.getId()).role(user.getRole()).username(user.getUsername()).build();

            // 3-a-3. 서비스 토큰 관리
            String accessToken = jwtUtil.createAccessToken(tokenCreateRequest);
            String refreshToken = jwtUtil.createRefreshToken(tokenCreateRequest);
            userTokenService.saveOrUpdateToken(refreshToken, jwtUtil.getRefreshTokenExpiration());
            LoginResponse loginResponse = LoginResponse.builder()
                    .userId(user.getId())
                    .role(user.getRole())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

            // 3-a-4. 로그인 응답 DTO 반환
            if (user.getNickname() == null ||user.getRole() == null) {
                // 회원가입 절차가 진행중인 경우(닉네임이나 역할이 null인 경우) 202 반환
                return ResponseEntity.accepted().body(loginResponse);
            } else {
                // 회원가입 절차가 완료된 경우 200 반환
                return ResponseEntity.ok().body(loginResponse);
            }
        } else {
            // 3-b. 유저 정보가 없으면 회원가입

            // 3-b-1. DB 저장을 위한 User 엔티티 생성
            String username = service.getUsername(userInfo);
            if (username == null) {
                // Apple 로그인의 경우 클라이언트를 통해 유저 이름 수신
                if (nameVerifyUtil.verifyName(loginRequest.getUsername())) {
                    username = loginRequest.getUsername();
                } else {
                    throw new InvalidNameException("잘못된 사용자 이름입니다.");
                }
            }

            // 3-b-2. DB에 유저 생성 및 토큰 관리
            UserCreateRequest userCreateRequest = UserCreateRequest.builder()
                    .provider(provider)
                    .providerId(providerId)
                    .username(username)
                    .socialRefreshToken(socialRefreshToken)
                    .build();
            LoginResponse loginResponse = userService.startCreateUser(userCreateRequest);

            URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/users/profiles")
                    .build()
                    .toUri();
            return ResponseEntity.created(location).body(loginResponse);
        }
    }

    @PatchMapping("/login/register")
    @Operation(summary = "회원가입", description = "닉네임, 역할을 받아서 회원가입 진행")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 절차 완료", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 유저 이름", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "50" +
                    "0", description = "서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "회원가입을 위한 요청 데이터", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterRequest.class)))
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        Long userId = identityService.getCurrentUserId();

        try {
            UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                    .userId(userId)
                    .nickname(registerRequest.getNickname())
                    .role(Role.valueOf(registerRequest.getRole()))
                    .build();

            // DB에 닉네임, 역할 필드 채움
            RegisterResponse response = userService.completeCreateUser(userRegisterRequest);

            return ResponseEntity.ok().body(response);
        } catch(IllegalArgumentException e) {
            throw new InvalidArgumentException();
        }
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

        TokenCreateRequest tokenCreateRequest = TokenCreateRequest.builder().userId(user.getId()).role(user.getRole()).username(user.getUsername()).build();

        String accessToken = jwtUtil.createAccessToken(tokenCreateRequest);
        String newRefreshToken = jwtUtil.createRefreshToken(tokenCreateRequest);
        userTokenService.saveOrUpdateToken(newRefreshToken, jwtUtil.getRefreshTokenExpiration());

        RefreshTokenResponse refreshTokenResponse = RefreshTokenResponse.builder().accessToken(accessToken).refreshToken(newRefreshToken).build();

        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").header(HttpHeaders.PRAGMA, "no-cache").body(refreshTokenResponse);
    }
}
