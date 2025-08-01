package com.moup.server.service;

public interface AuthService {
    boolean validateToken(String token);
}
