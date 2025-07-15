package com.moup.moup_server.model.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    private String provider;
    private String providerId;
}
