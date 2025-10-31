package com.moup.server.service;

import com.moup.server.exception.UserNotFoundException;
import com.moup.server.model.dto.CustomUserDetails;
import com.moup.server.model.entity.User;
import com.moup.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 파라미터로 넘어온 username을 "User ID"로 간주하고 Long으로 변환
        long userId;
        try {
            userId = Long.parseLong(username);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Invalid user identifier (not a Long): " + username);
        }

        // 2. findByUsername 대신 findById로 유저를 조회
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return new CustomUserDetails(user);
    }
}
