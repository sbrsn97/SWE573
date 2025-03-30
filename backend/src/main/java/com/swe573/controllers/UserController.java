package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.UserDTO;
import com.swe573.dto.UserRegistrationDTO;
import com.swe573.dto.UserLoginDTO;
import com.swe573.dto.UserUpdateDTO;
import com.swe573.dto.PasswordChangeDTO;
import com.swe573.models.User;
import com.swe573.services.UserService;
import com.swe573.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import com.swe573.exceptions.ResourceNotFoundException;
import com.swe573.exceptions.UnauthorizedException;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Users", description = "APIs for managing user accounts and profiles")
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @Operation(summary = "Test endpoint", description = "Simple endpoint to test if the server is running")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        System.out.println("Test endpoint hit!");
        return ResponseEntity.ok("Server is running!");
    }

    @Operation(summary = "Register new user", description = "Creates a new user account")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> registerUser(
            @Parameter(description = "User registration data", required = true) @RequestBody UserRegistrationDTO registrationDTO) {
        User user = userService.registerUser(registrationDTO);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", convertToDTO(user)));
    }

    @Operation(summary = "User login", description = "Authenticates a user and returns user data")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDTO>> loginUser(
            @Parameter(description = "User login credentials", required = true) @RequestBody UserLoginDTO loginDTO) {
        User user = userService.loginUser(loginDTO);
        return ResponseEntity.ok(ApiResponse.success("Login successful", convertToDTO(user)));
    }

    @Operation(summary = "Get user by ID", description = "Retrieves a user's profile by their ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUser(
            @Parameter(description = "ID of the user to retrieve", required = true) @PathVariable Long id) {
        User user = userService.getUser(id);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(user)));
    }

    @Operation(summary = "Get all users", description = "Retrieves a list of all users")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDTO> dtos = users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Update user profile", description = "Updates a user's profile information")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @Parameter(description = "ID of the user to update", required = true) @PathVariable Long id,
            @Parameter(description = "Updated user data", required = true) @RequestBody UserUpdateDTO updateDTO) {
        User user = userService.updateUser(id, updateDTO);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", convertToDTO(user)));
    }

    @Operation(summary = "Change password", description = "Changes a user's password")
    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Parameter(description = "ID of the user", required = true) @PathVariable Long id,
            @Parameter(description = "Password change data", required = true) @RequestBody PasswordChangeDTO passwordDTO) {
        userService.changePassword(id, passwordDTO);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @Operation(summary = "Delete user", description = "Deletes a user account")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        User currentUser = authenticationService.getCurrentUser();
        User targetUser = userService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getId().equals(id)) {
            // Self-deletion
            targetUser.deactivateAccount();
        } else if (authenticationService.isAdmin()) {
            // Admin deletion
            targetUser.deactivateByAdmin();
        } else {
            throw new UnauthorizedException("You don't have permission to delete this user");
        }

        userService.save(targetUser);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", null));
    }

    @Operation(summary = "Follow user", description = "Makes one user follow another user")
    @PostMapping("/{id}/follow/{followedId}")
    public ResponseEntity<ApiResponse<Void>> followUser(
            @Parameter(description = "ID of the follower", required = true) @PathVariable Long id,
            @Parameter(description = "ID of the user to follow", required = true) @PathVariable Long followedId) {
        userService.followUser(id, followedId);
        return ResponseEntity.ok(ApiResponse.success("User followed successfully", null));
    }

    @Operation(summary = "Unfollow user", description = "Makes one user unfollow another user")
    @PostMapping("/{id}/unfollow/{followedId}")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(
            @Parameter(description = "ID of the follower", required = true) @PathVariable Long id,
            @Parameter(description = "ID of the user to unfollow", required = true) @PathVariable Long followedId) {
        userService.unfollowUser(id, followedId);
        return ResponseEntity.ok(ApiResponse.success("User unfollowed successfully", null));
    }

    @Operation(summary = "Get current user", description = "Retrieves the profile of the currently authenticated user")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(user)));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(@PathVariable Long id) {
        User currentUser = authenticationService.getCurrentUser();
        User targetUser = userService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!targetUser.canBeReactivatedBy(currentUser)) {
            throw new UnauthorizedException("You don't have permission to reactivate this user");
        }

        targetUser.reactivateAccount();
        userService.save(targetUser);
        return ResponseEntity.ok(ApiResponse.success("User reactivated successfully", null));
    }

    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> hardDeleteUser(@PathVariable Long id) {
        User targetUser = userService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        targetUser.hardDelete();
        userService.delete(targetUser);
        return ResponseEntity.ok(ApiResponse.success("User permanently deleted", null));
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