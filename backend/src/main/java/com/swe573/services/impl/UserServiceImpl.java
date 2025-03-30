package com.swe573.services.impl;

import com.swe573.dto.UserDTO;
import com.swe573.exceptions.DuplicateResourceException;
import com.swe573.models.User;
import com.swe573.models.enums.Role;
import com.swe573.repositories.UserRepository;
import com.swe573.services.UserService;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    private void setupAdmin() {
        if(userCount() == 0) {
            UserDTO userDTO = new UserDTO();
            userDTO.setUsername("admin");
            userDTO.setPassword("123456"); // Raw password, will be encoded in createUser
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

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return passwordEncoder.matches(password, user.getPassword());
    }

    private void validateUniqueConstraints(UserDTO userDTO) {
        if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already exists");
        }
    }

    private void updateUserFromDTO(User user, UserDTO userDTO) {
        user.setUsername(userDTO.getUsername());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setRole(userDTO.getRole());
        user.setBirthDate(userDTO.getBirthDate());
        user.setBio(userDTO.getBio());
        user.setLocation(userDTO.getLocation());
        user.setProfession(userDTO.getProfession());
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            String hashedPassword = passwordEncoder.encode(userDTO.getPassword());
            user.setPassword(hashedPassword);
        }
    }
} 