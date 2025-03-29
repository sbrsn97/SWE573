package com.swe573.controllers;

import com.swe573.dto.ApiResponse;
import com.swe573.dto.UserDTO;
import com.swe573.models.User;
import com.swe573.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody UserDTO userDTO) {
        User user = userService.createUser(userDTO);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long id) {
        User user = userService.getUser(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        User user = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
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
    public ResponseEntity<ApiResponse<User>> getCurrentUser() {
        System.out.println("Getting current user");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Authentication: " + (authentication != null ? authentication.getName() : "null"));
        System.out.println("Is authenticated: " + (authentication != null ? authentication.isAuthenticated() : "false"));
        System.out.println("Authorities: " + (authentication != null ? authentication.getAuthorities() : "null"));
        
        User user = userService.getCurrentUser();
        System.out.println("Found user: " + user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user));
    }
} 