package com.swe573.services.impl;

import com.google.common.hash.Hashing;
import com.swe573.dto.UserDTO;
import com.swe573.models.User;
import com.swe573.services.AuthenticationService;
import com.swe573.services.JwtService;
import com.swe573.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

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
        try {
            String hashedPassword = Hashing.sha256()
                    .hashString(password, StandardCharsets.UTF_8)
                    .toString();
            
            if (userService.authenticateUser(username, hashedPassword)) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                return jwtService.generateToken(userDetails);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean register(UserDTO userDTO) {
        String hashedPassword = Hashing.sha256()
                .hashString(userDTO.getPassword(), StandardCharsets.UTF_8)
                .toString();
        userDTO.setPassword(hashedPassword);
        return userService.registerUser(userDTO);
    }
} 