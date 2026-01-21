package com.moup.domain.auth.application;

import com.moup.domain.auth.domain.Login;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceFactory {

    private final Map<Login, AuthService> serviceMap;

    public AuthServiceFactory(List<AuthService> services) {
        this.serviceMap = services.stream()
                .collect(Collectors.toMap(AuthService::getProvider, Function.identity()));
    }

    public AuthService getService(Login login) {
        return serviceMap.get(login);
    }
}
