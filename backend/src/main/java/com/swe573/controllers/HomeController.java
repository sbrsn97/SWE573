package com.swe573.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Home", description = "APIs for application home and health check")
@RestController
public class HomeController {

    @Operation(summary = "Home page", description = "Returns a welcome message for the API")
    @GetMapping("/")
    public String home() {
        return "Welcome to Connect the Dots API!";
    }

    @Operation(summary = "Health check", description = "Returns the application health status")
    @GetMapping("/health")
    public String healthCheck() {
        return "Application is running!";
    }
} 