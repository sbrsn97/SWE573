package com.swe573.controllers;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.UserLoginDTO;
import com.swe573.dto.UserRegistrationDTO;
import com.swe573.services.AuthenticationService;

@Tag(name = "Authentication", description = "APIs for user authentication and registration")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationService authService;

    @Operation(summary = "Register user", description = "Registers a new user in the system")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(
            @Parameter(description = "User registration data", required = true) @RequestBody UserRegistrationDTO registrationDTO) {
        try {
            authService.register(registrationDTO);
            return ResponseEntity.ok(ApiResponse.success("User registered successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Login user", description = "Authenticates a user and returns a JWT token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @Parameter(description = "User login credentials", required = true) @RequestBody UserLoginDTO loginDTO) {
        try {
            String token = authService.authenticate(loginDTO.getUsername(), loginDTO.getPassword());
            if (token == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid credentials"));
            }
            Map<String, String> response = Map.of("token", token);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Logout user", description = "Logs out the current user")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // Since we're using JWT, the actual token invalidation happens on the client side
        // by removing the token from storage
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
