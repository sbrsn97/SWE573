package com.swe573.dto;

import java.time.LocalDate;

import com.swe573.models.enums.Role;

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
    private Role role = Role.USER;
} 