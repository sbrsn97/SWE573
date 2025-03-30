package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.UserDTO;
import com.swe573.dto.UserRegistrationDTO;
import com.swe573.dto.UserLoginDTO;
import com.swe573.dto.UserUpdateDTO;
import com.swe573.dto.PasswordChangeDTO;
import com.swe573.models.User;
import com.swe573.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        System.out.println("Test endpoint hit!");
        return ResponseEntity.ok("Server is running!");
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> registerUser(@RequestBody UserRegistrationDTO registrationDTO) {
        User user = userService.registerUser(registrationDTO);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", convertToDTO(user)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDTO>> loginUser(@RequestBody UserLoginDTO loginDTO) {
        User user = userService.loginUser(loginDTO);
        return ResponseEntity.ok(ApiResponse.success("Login successful", convertToDTO(user)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable Long id) {
        User user = userService.getUser(id);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDTO> dtos = users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateDTO updateDTO) {
        User user = userService.updateUser(id, updateDTO);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", convertToDTO(user)));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @RequestBody PasswordChangeDTO passwordDTO) {
        userService.changePassword(id, passwordDTO);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @PostMapping("/{id}/follow/{followedId}")
    public ResponseEntity<ApiResponse<Void>> followUser(@PathVariable Long id, @PathVariable Long followedId) {
        userService.followUser(id, followedId);
        return ResponseEntity.ok(ApiResponse.success("User followed successfully", null));
    }

    @PostMapping("/{id}/unfollow/{followedId}")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(@PathVariable Long id, @PathVariable Long followedId) {
        userService.unfollowUser(id, followedId);
        return ResponseEntity.ok(ApiResponse.success("User unfollowed successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(user)));
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setBio(user.getBio());
        dto.setLocation(user.getLocation());
        dto.setBirthDate(user.getBirthDate());
        dto.setProfession(user.getProfession());
        dto.setRole(user.getRole());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setReputation(user.getReputation());
        dto.setNotificationPreferences(user.getNotificationPreferences());
        return dto;
    }
} 