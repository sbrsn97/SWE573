package com.swe573.services;

import com.swe573.dto.UserDTO;

public interface AuthenticationService {
    String authenticate(String username, String password);
    boolean register(UserDTO userDTO);
} 