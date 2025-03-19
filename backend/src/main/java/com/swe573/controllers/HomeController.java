package com.swe573.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Welcome to Connect the Dots API!";
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "Application is running!";
    }
} 