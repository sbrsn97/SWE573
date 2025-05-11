package com.swe573.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import com.swe573.models.enums.NotificationType;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Data transfer object for updating user profile")
public class UserUpdateDTO {
    
    @Schema(description = "User's first name", example = "John")
    private String firstName;
    
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;
    
    @Size(max = 500, message = "Bio must not exceed 500 characters")
    @Schema(description = "User's biography", example = "Software developer with 5 years of experience")
    private String bio;
    
    @Schema(description = "User's location", example = "New York, USA")
    private String location;
    
    @Schema(description = "User's birth date", example = "1990-01-01")
    private LocalDate birthDate;
    
    @Schema(description = "User's profession", example = "Software Engineer")
    private String profession;
    
    @Schema(description = "User's notification preferences")
    private Map<NotificationType, Boolean> notificationPreferences;
    
    @Schema(description = "List of tag IDs associated with the user")
    private Set<Long> tagIds;
} 