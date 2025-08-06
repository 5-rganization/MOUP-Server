package com.moup.server.service;

import com.moup.server.common.Login;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Map;

public interface AuthService {
    Login getProvider();
    Map<String, Object> verifyIdToken(String idToken) throws GeneralSecurityException, IOException, ParseException;
}
