package com.swe573.controllers;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.hash.Hashing;
import com.swe573.dto.ApiResponse;
import com.swe573.dto.UserDTO;
import com.swe573.models.auth.AuthForm;
import com.swe573.services.UserService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> Login(@RequestBody AuthForm form) {
        String hashedPw = Hashing.sha256()
            .hashString(form.getPassword(), StandardCharsets.UTF_8)
            .toString();

        if (!userService.authenticateUser(form.getUsername(), hashedPw)) {
            return ResponseEntity.ok(ApiResponse.error("Invalid username or password", null));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Login Successful", null));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> Register(@RequestBody AuthForm form) {
        String hashedPw = Hashing.sha256()
            .hashString(form.getPassword(), StandardCharsets.UTF_8)
            .toString();

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(form.getUsername());
        userDTO.setPassword(hashedPw);
        userDTO.setFirstName(form.getFirstName());
        userDTO.setLastName(form.getLastName());
        userDTO.setEmail(form.getEmail());

        if(!userService.registerUser(userDTO)) {
            return ResponseEntity.ok(ApiResponse.error("Username or Email already exists", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Register Successful", null));
    }

}
