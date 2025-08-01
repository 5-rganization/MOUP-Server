package com.moup.server.service;

import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService implements AuthService {
    @Override
    public boolean validateToken(String token) {

        return false;
    }
}
