package com.swe573.services;

import com.swe573.dto.UserRegistrationDTO;
import com.swe573.models.User;

public interface AuthenticationService {
    String authenticate(String username, String password);
    User register(UserRegistrationDTO registrationDTO);
    User getCurrentUser();
    boolean isAdmin();
} 