package com.swe573.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import com.swe573.models.enums.NotificationType;
import com.swe573.models.enums.Role;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "User data transfer object for general user information")
public class UserDTO {
    @Schema(description = "Unique identifier of the user")
    private Long id;
    
    @Schema(description = "Username of the user")
    private String username;
    
    @Schema(description = "Email address of the user")
    private String email;
    
    @Schema(description = "First name of the user")
    private String firstName;
    
    @Schema(description = "Last name of the user")
    private String lastName;
    
    @Schema(description = "User's biography")
    private String bio;
    
    @Schema(description = "User's location")
    private String location;
    
    @Schema(description = "User's birth date")
    private LocalDate birthDate;
    
    @Schema(description = "User's profession")
    private String profession;
    
    @Schema(description = "User's role in the system")
    private Role role;
    
    @Schema(description = "User's last login timestamp")
    private LocalDateTime lastLoginAt;
    
    @Schema(description = "User's reputation score")
    private int reputation;
    
    @Schema(description = "User's notification preferences")
    private Map<NotificationType, Boolean> notificationPreferences;
} 