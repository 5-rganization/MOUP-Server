package com.moup.server.model.dto;

import lombok.Builder;

@Builder
public class GoogleLoginResponse {
    private String userId;
    private String email;
    private Boolean emailVerified;
    private String name;
    private String pictureUrl;
    private String locale;
    private String familyName;
    private String givenName;
}
