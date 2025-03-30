package com.swe573.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Data transfer object for user login")
public class UserLoginDTO {
    
    @NotBlank(message = "Username is required")
    @Schema(description = "Username for login", example = "johndoe")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Schema(description = "Password for login", example = "securePassword123")
    private String password;
} 