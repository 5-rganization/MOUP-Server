package com.moup.server.service;

import com.moup.server.common.Login;
import com.moup.server.exception.DuplicateUserException;
import com.moup.server.exception.UserNotFoundException;
import com.moup.server.model.dto.RegisterRequest;
import com.moup.server.model.entity.User;
import com.moup.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
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

    public User findByProviderAndId(Login provider, String providerId) {
        return userRepository.findByProviderAndId(provider, providerId)
                .orElseThrow(UserNotFoundException::new);
    }

    public User findByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    public void updateProfileImg(Long userId, String profileImg) {
        userRepository.updateProfileImg(userId, profileImg);
    }
}
