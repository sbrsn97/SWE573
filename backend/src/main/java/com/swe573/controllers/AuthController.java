package com.swe573.controllers;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.UserLoginDTO;
import com.swe573.dto.UserRegistrationDTO;
import com.swe573.services.AuthenticationService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(@RequestBody UserRegistrationDTO registrationDTO) {
        try {
            authService.register(registrationDTO);
            return ResponseEntity.ok(ApiResponse.success("User registered successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody UserLoginDTO loginDTO) {
        try {
            String token = authService.authenticate(loginDTO.getUsername(), loginDTO.getPassword());
            Map<String, String> response = Map.of("token", token);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
