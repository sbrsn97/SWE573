package com.swe573.services.impl;

import com.google.common.hash.Hashing;
import com.swe573.dto.UserDTO;
import com.swe573.models.User;
import com.swe573.repositories.UserRepository;
import com.swe573.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public User createUser(UserDTO userDTO) {
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
        User user = getUser(id);
        updateUserFromDTO(user, userDTO);
        return userRepository.save(user);
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

    private void updateUserFromDTO(User user, UserDTO userDTO) {
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPassword(userDTO.getPassword()); // Note: In production, this should be hashed
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setBio(userDTO.getBio());
        user.setLocation(userDTO.getLocation());
    }

    private void authenticateUser(String username, String password) {
        String hashedPw = Hashing.sha256()
            .hashString(password, StandardCharsets.UTF_8)
            .toString();
        User user = userRepository.findOne(User, )
    }
} 