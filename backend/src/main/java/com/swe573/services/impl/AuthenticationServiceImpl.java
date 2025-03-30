package com.swe573.services.impl;

import com.swe573.dto.UserDTO;
import com.swe573.dto.UserRegistrationDTO;
import com.swe573.models.User;
import com.swe573.services.AuthenticationService;
import com.swe573.services.JwtService;
import com.swe573.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public String authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return null;
        }
        try {
            // Use AuthenticationManager to validate credentials
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            
            if (authentication.isAuthenticated()) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                return jwtService.generateToken(userDetails);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public User register(UserRegistrationDTO registrationDTO) {
        return userService.registerUser(registrationDTO);
    }
} 