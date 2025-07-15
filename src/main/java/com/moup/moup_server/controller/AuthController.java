package com.moup.moup_server.controller;

import com.moup.moup_server.model.DTO.LoginRequest;
import com.moup.moup_server.model.entity.User;
import com.moup.moup_server.service.UserService;
import com.moup.moup_server.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    private final JwtUtil jwtUtil;

    @Operation(summary = "로그인", description = "소셜 로그인 타입과 아이디를 입력 받아서 로그인")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "로그인 실패"),
            @ApiResponse(responseCode = "404", description = "등록된 아이디 없음")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 로그인 auth 로직
            User user = userService.findByProviderId(loginRequest.getProviderId());
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("errorMsg", "등록되지 않은 유저입니다."));
            }

            String token = jwtUtil.createToken(user.getId(), user.getRole().toString());

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

//    @Operation(summary = "회원가입", description = "아이디, 외부 로그인 정보, 역할을 받아서 회원가입")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "회원 가입 성공"),
//            @ApiResponse(responseCode = "400", description = "회원 가입 실패")
//    })
//    @PostMapping("/register")
//    public int createUser(@ModelAttribute User user) {
//        return userService.createUser(user);
//    }
}
