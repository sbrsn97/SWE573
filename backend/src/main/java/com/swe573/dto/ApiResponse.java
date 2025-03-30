package com.swe573.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {
    @Schema(description = "Whether the operation was successful")
    private boolean success;
    
    @Schema(description = "Message describing the result of the operation")
    private String message;
    
    @Schema(description = "Data payload of the response")
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(true, "Operation successful", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<T>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<T>(false, message, null);
    }
} 