package com.swe573.services;

import com.swe573.dto.UserDTO;
import com.swe573.dto.UserRegistrationDTO;
import com.swe573.dto.UserLoginDTO;
import com.swe573.dto.UserUpdateDTO;
import com.swe573.dto.PasswordChangeDTO;
import com.swe573.models.User;

import java.util.List;

public interface UserService {
    User registerUser(UserRegistrationDTO registrationDTO);
    User loginUser(UserLoginDTO loginDTO);
    User getUser(Long id);
    List<User> getAllUsers();
    User updateUser(Long id, UserUpdateDTO updateDTO);
    void changePassword(Long id, PasswordChangeDTO passwordDTO);
    void deleteUser(Long id);
    void followUser(Long followerId, Long followedId);
    void unfollowUser(Long followerId, Long followedId);
    User getCurrentUser();
    long userCount();
} 