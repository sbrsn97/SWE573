package com.swe573.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Data transfer object for user registration")
public class UserRegistrationDTO {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Username for the new account", example = "johndoe")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Email address for the new account", example = "john.doe@example.com")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(description = "Password for the new account", example = "securePassword123")
    private String password;
    
    @NotBlank(message = "First name is required")
    @Schema(description = "User's first name", example = "John")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;
    
    @Schema(description = "User's biography", example = "Software developer with 5 years of experience")
    private String bio;
    
    @Schema(description = "User's location", example = "New York, USA")
    private String location;
    
    @Schema(description = "User's birth date", example = "1990-01-01")
    private LocalDate birthDate;
    
    @Schema(description = "User's profession", example = "Software Engineer")
    private String profession;
} 