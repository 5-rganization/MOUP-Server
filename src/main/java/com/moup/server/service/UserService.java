package com.moup.server.service;

import com.moup.server.exception.DuplicateUserException;
import com.moup.server.exception.ErrorCode;
import com.moup.server.exception.UserNotFoundException;
import com.moup.server.model.dto.RegisterRequest;
import com.moup.server.model.entity.User;
import com.moup.server.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public void createUser(RegisterRequest registerRequest) {
        try {
            userRepository.createUser(registerRequest);
        } catch (DuplicateKeyException e) {
            throw new DuplicateUserException();
        }
    }

    public User findByProviderId(String providerId) {
        return userRepository.findByProviderId(providerId)
                .orElseThrow(UserNotFoundException::new);
    }

    public User findByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }
}
