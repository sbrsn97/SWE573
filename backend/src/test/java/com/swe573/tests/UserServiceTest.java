package com.swe573.tests;

import com.swe573.models.User;
import com.swe573.repositories.UserRepository;
import com.swe573.services.impl.UserServiceImpl;
import com.swe573.dto.UserRegistrationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserRegistrationDTO testRegistrationDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        
        testRegistrationDTO = new UserRegistrationDTO();
        testRegistrationDTO.setUsername("testuser");
        testRegistrationDTO.setEmail("test@example.com");
        testRegistrationDTO.setPassword("password123");
        testRegistrationDTO.setFirstName("Test");
        testRegistrationDTO.setLastName("User");
        
        // Setup SecurityContext mock
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getCurrentUser_Success() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getCurrentUser_UserNotFound() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("nonexistentuser");
        when(userRepository.findByUsername("nonexistentuser")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.getCurrentUser());
        verify(userRepository).findByUsername("nonexistentuser");
    }

    @Test
    void getCurrentUser_NoAuthentication() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.getCurrentUser());
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void registerUser_Success() {
        // Arrange
        when(userRepository.findByUsername(testRegistrationDTO.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(testRegistrationDTO.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(testRegistrationDTO.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.registerUser(testRegistrationDTO);

        // Assert
        assertNotNull(result);
        verify(userRepository).findByUsername(testRegistrationDTO.getUsername());
        verify(userRepository).findByEmail(testRegistrationDTO.getEmail());
        verify(passwordEncoder).encode(testRegistrationDTO.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_DuplicateUsername() {
        // Arrange
        when(userRepository.findByUsername(testRegistrationDTO.getUsername())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.registerUser(testRegistrationDTO));
        verify(userRepository).findByUsername(testRegistrationDTO.getUsername());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void registerUser_DuplicateEmail() {
        // Arrange
        when(userRepository.findByUsername(testRegistrationDTO.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(testRegistrationDTO.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.registerUser(testRegistrationDTO));
        verify(userRepository).findByUsername(testRegistrationDTO.getUsername());
        verify(userRepository).findByEmail(testRegistrationDTO.getEmail());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }
} 