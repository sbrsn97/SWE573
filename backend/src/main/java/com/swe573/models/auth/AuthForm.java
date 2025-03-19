package com.swe573.models.auth;

import lombok.Getter;

public class AuthForm {
    @Getter
    private String username;
    @Getter
    private String password;
    @Getter
    private String firstName;
    @Getter
    private String lastName;
    @Getter
    private String email;

    public AuthForm(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
