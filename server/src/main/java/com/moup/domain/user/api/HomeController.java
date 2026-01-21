package com.moup.domain.user.api;

import com.moup.domain.user.domain.User;
import com.moup.domain.user.dto.BaseHomeResponse;
import com.moup.domain.user.application.HomeService;
import com.moup.global.security.IdentityService;
import com.moup.domain.user.application.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static com.moup.global.common.TimeConstants.SEOUL_ZONE_ID;

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

        BaseHomeResponse response = homeService.getHomeInfo(user, LocalDate.now(SEOUL_ZONE_ID));
        return ResponseEntity.ok().body(response);
    }

}
