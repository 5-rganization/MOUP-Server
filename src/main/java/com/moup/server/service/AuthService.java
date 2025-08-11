package com.moup.server.service;

import com.moup.server.common.Login;
import com.moup.server.exception.InvalidTokenException;
import java.util.Map;

public interface AuthService {
    Login getProvider();
    Map<String, Object> verifyIdToken(String idToken) throws InvalidTokenException;
}
