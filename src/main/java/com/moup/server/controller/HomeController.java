package com.moup.server.controller;

import com.moup.server.model.dto.BaseHomeResponse;
import com.moup.server.model.entity.User;
import com.moup.server.service.HomeService;
import com.moup.server.service.IdentityService;
import com.moup.server.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Home", description = "홈 화면 정보 관리 API 엔드포인트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/home")
public class HomeController implements HomeSpecification {
    private final IdentityService identityService;
    private final UserService userService;
    private final HomeService homeService;

    @Override
    @GetMapping
    public ResponseEntity<?> getTodayHomeInfo() {
        Long userId = identityService.getCurrentUserId();
        User user = userService.findUserById(userId);

        BaseHomeResponse response = homeService.getHomeInfo(user, LocalDate.now());
        return ResponseEntity.ok().body(response);
    }

}
