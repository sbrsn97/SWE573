package com.swe573.models.auth;

import lombok.Getter;

public class AuthForm {
    @Getter
    private String username;
    @Getter
    private String password;

    public AuthForm(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
