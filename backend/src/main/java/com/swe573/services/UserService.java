package com.swe573.services;

import com.swe573.dto.UserDTO;
import com.swe573.models.User;

import java.util.List;

public interface UserService {
    User createUser(UserDTO userDTO);
    User getUser(Long id);
    List<User> getAllUsers();
    User updateUser(Long id, UserDTO userDTO);
    void deleteUser(Long id);
    void followUser(Long followerId, Long followedId);
    void unfollowUser(Long followerId, Long followedId);
    boolean authenticateUser(String username, String hashedPassword);
    boolean registerUser(UserDTO userDTO);
} 