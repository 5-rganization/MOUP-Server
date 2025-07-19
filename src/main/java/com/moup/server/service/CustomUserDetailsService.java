package com.moup.server.service;

import com.moup.server.exception.ErrorCode;
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);
        return new CustomUserDetails(user);
    }
}
