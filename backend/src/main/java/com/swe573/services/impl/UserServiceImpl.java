package com.swe573.services.impl;

import com.google.common.hash.Hashing;
import com.swe573.dto.UserDTO;
import com.swe573.exceptions.DuplicateResourceException;
import com.swe573.models.User;
import com.swe573.models.enums.Role;
import com.swe573.repositories.UserRepository;
import com.swe573.services.UserService;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @PostConstruct
    private void setupAdmin() {
        String hashedPw = Hashing.sha256()
            .hashString("123456", StandardCharsets.UTF_8)
            .toString();
        if(userCount() == 0) {
            UserDTO userDTO = new UserDTO();
            userDTO.setUsername("admin");
            userDTO.setPassword(hashedPw);
            userDTO.setFirstName("Admin");
            userDTO.setLastName("Admin");
            userDTO.setEmail("admin@admin.com");
            userDTO.setRole(Role.ADMIN);
            registerUser(userDTO);
            System.out.println("Admin user created");
            return;
        }
        System.out.println("Admin user already exists");
    }

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public User createUser(UserDTO userDTO) {
        validateUniqueConstraints(userDTO);
        User user = new User();
        updateUserFromDTO(user, userDTO);
        return userRepository.save(user);
    }

    @Override
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User updateUser(Long id, UserDTO userDTO) {
        User existingUser = getUser(id);
        // Only check for duplicates if username or email is being changed
        if (!existingUser.getUsername().equals(userDTO.getUsername()) || 
            !existingUser.getEmail().equals(userDTO.getEmail())) {
            validateUniqueConstraints(userDTO);
        }
        updateUserFromDTO(existingUser, userDTO);
        return userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getUser(id);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void followUser(Long followerId, Long followedId) {
        User follower = getUser(followerId);
        User followed = getUser(followedId);
        
        follower.getFollowing().add(followed);
        followed.getFollowers().add(follower);
        
        userRepository.save(follower);
        userRepository.save(followed);
    }

    @Override
    @Transactional
    public void unfollowUser(Long followerId, Long followedId) {
        User follower = getUser(followerId);
        User followed = getUser(followedId);
        
        follower.getFollowing().remove(followed);
        followed.getFollowers().remove(follower);
        
        userRepository.save(follower);
        userRepository.save(followed);
    }

    @Override
    public boolean authenticateUser(String username, String password) {
        return userRepository.findByUsername(username)
                .map(user -> user.getPassword().equals(password))
                .orElse(false);
    }

    @Override
    public boolean registerUser(UserDTO userDTO) {
        try {
            validateUniqueConstraints(userDTO);
            createUser(userDTO);
            return true;
        } catch (DuplicateResourceException e) {
            return false;
        }
    }

    @Override
    public long userCount() {
        return userRepository.count();
    }

    private void validateUniqueConstraints(UserDTO userDTO) {
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new DuplicateResourceException("Username '" + userDTO.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new DuplicateResourceException("Email '" + userDTO.getEmail() + "' is already registered");
        }
    }

    private void updateUserFromDTO(User user, UserDTO userDTO) {
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPassword(userDTO.getPassword());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setBio(userDTO.getBio());
        user.setLocation(userDTO.getLocation());
        if (userDTO.getRole() != null) {
            user.setRole(userDTO.getRole());
        }
    }
} 