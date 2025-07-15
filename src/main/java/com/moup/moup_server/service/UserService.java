package com.moup.moup_server.service;

import com.moup.moup_server.model.entity.User;
import com.moup.moup_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public int createUser(User user) {
        return userRepository.createUser(user);
    }

    public User findByProviderId(String providerId) {
        return userRepository.findByProviderId(providerId);
    }

    public int register(User user) {
        return userRepository.createUser(user);
    }
}
