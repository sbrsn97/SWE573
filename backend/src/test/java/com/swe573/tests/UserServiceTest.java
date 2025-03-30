package com.swe573.tests;

import com.swe573.models.User;
import com.swe573.repositories.UserRepository;
import com.swe573.services.impl.UserServiceImpl;
import com.swe573.dto.UserDTO;
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
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        
        testUserDTO = new UserDTO();
        testUserDTO.setUsername("testuser");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setPassword("password123");
        testUserDTO.setFirstName("Test");
        testUserDTO.setLastName("User");
        
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
    void authenticateUser_Success() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);

        // Act
        boolean result = userService.authenticateUser(username, password);

        // Assert
        assertTrue(result);
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, testUser.getPassword());
    }

    @Test
    void authenticateUser_UserNotFound() {
        // Arrange
        String username = "nonexistentuser";
        String password = "password123";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.authenticateUser(username, password));
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void authenticateUser_WrongPassword() {
        // Arrange
        String username = "testuser";
        String wrongPassword = "wrongpassword";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongPassword, testUser.getPassword())).thenReturn(false);

        // Act
        boolean result = userService.authenticateUser(username, wrongPassword);

        // Assert
        assertFalse(result);
        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(wrongPassword, testUser.getPassword());
    }

    @Test
    void registerUser_Success() {
        // Arrange
        when(userRepository.findByUsername(testUserDTO.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(testUserDTO.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(testUserDTO.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        boolean result = userService.registerUser(testUserDTO);

        // Assert
        assertTrue(result);
        verify(userRepository, times(2)).findByUsername(testUserDTO.getUsername());
        verify(userRepository, times(2)).findByEmail(testUserDTO.getEmail());
        verify(passwordEncoder).encode(testUserDTO.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_DuplicateUsername() {
        // Arrange
        when(userRepository.findByUsername(testUserDTO.getUsername())).thenReturn(Optional.of(testUser));

        // Act
        boolean result = userService.registerUser(testUserDTO);

        // Assert
        assertFalse(result);
        verify(userRepository).findByUsername(testUserDTO.getUsername());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void registerUser_DuplicateEmail() {
        // Arrange
        when(userRepository.findByUsername(testUserDTO.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(testUserDTO.getEmail())).thenReturn(Optional.of(testUser));

        // Act
        boolean result = userService.registerUser(testUserDTO);

        // Assert
        assertFalse(result);
        verify(userRepository).findByUsername(testUserDTO.getUsername());
        verify(userRepository).findByEmail(testUserDTO.getEmail());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }
} 