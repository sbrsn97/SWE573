package com.swe573.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class UserDTO {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String bio;
    private String location;
    private LocalDate birthDate;
} 