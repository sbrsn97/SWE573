package com.swe573.services.impl;

import com.swe573.dto.UserRegistrationDTO;
import com.swe573.dto.UserLoginDTO;
import com.swe573.dto.UserUpdateDTO;
import com.swe573.dto.PasswordChangeDTO;
import com.swe573.exceptions.DuplicateResourceException;
import com.swe573.exceptions.InvalidCredentialsException;
import com.swe573.exceptions.ResourceNotFoundException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override 
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public void delete(User user) {
        userRepository.delete(user);
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    private void setupAdmin() {
        if(userCount() == 0) {
            UserRegistrationDTO adminDTO = new UserRegistrationDTO();
            adminDTO.setUsername("admin");
            adminDTO.setPassword("123456"); // Raw password, will be encoded in registerUser
            adminDTO.setFirstName("Admin");
            adminDTO.setLastName("Admin");
            adminDTO.setEmail("admin@admin.com");
            registerUser(adminDTO);
            System.out.println("Admin user created");
            return;
        }
        System.out.println("Admin user already exists");
    }

    @Override
    @Transactional
    public User registerUser(UserRegistrationDTO registrationDTO) {
        validateUniqueConstraints(registrationDTO);
        User user = new User();
        updateUserFromRegistrationDTO(user, registrationDTO);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User loginUser(UserLoginDTO loginDTO) {
        User user = userRepository.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));
        
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User updateUser(Long id, UserUpdateDTO updateDTO) {
        User existingUser = getUser(id);
        updateUserFromUpdateDTO(existingUser, updateDTO);
        return userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public void changePassword(Long id, PasswordChangeDTO passwordDTO) {
        User user = getUser(id);
        
        if (!passwordEncoder.matches(passwordDTO.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        
        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            throw new InvalidCredentialsException("New passwords do not match");
        }
        
        user.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
        userRepository.save(user);
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
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user found");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public long userCount() {
        return userRepository.count();
    }

    @Override
    public List<User> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword);
    }

    private void validateUniqueConstraints(UserRegistrationDTO registrationDTO) {
        if (userRepository.findByUsername(registrationDTO.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.findByEmail(registrationDTO.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already exists");
        }
    }

    private void updateUserFromRegistrationDTO(User user, UserRegistrationDTO dto) {
        user.setUsername(dto.getUsername());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setBirthDate(dto.getBirthDate());
        user.setBio(dto.getBio());
        user.setLocation(dto.getLocation());
        user.setProfession(dto.getProfession());
        user.setRole(Role.USER);
    }

    private void updateUserFromUpdateDTO(User user, UserUpdateDTO dto) {
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setBirthDate(dto.getBirthDate());
        user.setBio(dto.getBio());
        user.setLocation(dto.getLocation());
        user.setProfession(dto.getProfession());
        user.setNotificationPreferences(dto.getNotificationPreferences());
    }
} 