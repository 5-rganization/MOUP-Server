package com.moup.moup_server.controller;

import com.moup.moup_server.model.dto.LoginRequest;
import com.moup.moup_server.model.dto.RegisterRequest;
import com.moup.moup_server.model.entity.User;
import com.moup.moup_server.service.UserService;
import com.moup.moup_server.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "소셜 로그인 타입과 아이디를 입력 받아서 로그인")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "로그인 실패"),
            @ApiResponse(responseCode = "404", description = "등록된 유저 ID 없음")
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
        try {
            // 로그인 auth 로직
            User user = userService.findByProviderId(loginRequest.getProviderId());

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("errorMsg", "등록되지 않은 유저입니다."));
            }

            String token;
            try {
                token = jwtUtil.createToken(user.getId(), user.getRole().toString());
            } catch (DuplicateKeyException e) {
                throw new Exception();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(Map.of("userId", user.getId()));
        } catch (Exception e) {
            e.getStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("errorMsg", "로그인 실패"));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "소셜 로그인 정보, 닉네임, 역할을 받아서 회원가입")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "회원가입 실패"),
            @ApiResponse(responseCode = "409", description = "중복된 유저 ID")
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
        try {
            try {
                userService.createUser(registerRequest);
            } catch (DuplicateKeyException e) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errorMsg", "이미 가입된 유저입니다."));
            }

            User user = userService.findByProviderId(registerRequest.getProviderId());

            String token = jwtUtil.createToken(user.getId(), user.getRole().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(Map.of("userId", user.getId()));
        } catch (Exception e) {
            e.getStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("errorMsg", "회원가입 실패"));
        }
    }
}
