package com.swe573.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Data transfer object for changing user password")
public class PasswordChangeDTO {
    
    @NotBlank(message = "Current password is required")
    @Schema(description = "User's current password", example = "currentPassword123")
    private String currentPassword;
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    @Schema(description = "User's new password", example = "newSecurePassword123")
    private String newPassword;
    
    @NotBlank(message = "Password confirmation is required")
    @Schema(description = "Confirmation of the new password", example = "newSecurePassword123")
    private String confirmPassword;
} 