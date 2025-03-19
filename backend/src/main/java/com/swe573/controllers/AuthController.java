package com.swe573.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swe573.dto.ApiResponse;
import com.swe573.models.auth.AuthForm;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> Login(@RequestBody AuthForm form) {
        
        
        return ResponseEntity.ok(ApiResponse.success("Login Successful", null));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> Register(@RequestBody AuthForm form) {

        return ResponseEntity.ok(ApiResponse.success("Register Successful", null));
    }

}
