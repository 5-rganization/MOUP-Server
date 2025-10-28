package com.moup.server.common;

public enum Login {
    LOGIN_GOOGLE,
    LOGIN_APPLE;

    @Override
    public String toString() {
        return switch (this) {
            case LOGIN_GOOGLE -> "Google";
            case LOGIN_APPLE -> "Apple";
        };
    }
}
