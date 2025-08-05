package com.moup.server.service;

import com.moup.server.common.Login;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

public interface AuthService {
    Login getProvider();
    String getProviderId(Map<String, Object> userInfo);
    String getUsername(Map<String, Object> userInfo);
    Map<String, Object> verifyIdToken(String idToken) throws GeneralSecurityException, IOException;
}
