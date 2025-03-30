package com.swe573.tests;

import com.swe573.models.User;
import com.swe573.dto.UserRegistrationDTO;
import com.swe573.dto.UserUpdateDTO;
import com.swe573.dto.PasswordChangeDTO;
import com.swe573.models.enums.Role;
import com.swe573.repositories.UserRepository;
import com.swe573.services.impl.UserServiceImpl;
import com.swe573.exceptions.InvalidCredentialsException;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

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
        testUser.setPassword("encodedNewPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.USER);
        
        testRegistrationDTO = new UserRegistrationDTO();
        testRegistrationDTO.setUsername("testuser");
        testRegistrationDTO.setEmail("test@example.com");
        testRegistrationDTO.setPassword("password");
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

    @Test
    void changePassword_Success() {
        // Arrange
        PasswordChangeDTO passwordDTO = new PasswordChangeDTO();
        passwordDTO.setCurrentPassword("oldPassword");
        passwordDTO.setNewPassword("newPassword");
        passwordDTO.setConfirmPassword("newPassword");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(passwordDTO.getCurrentPassword(), testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(passwordDTO.getNewPassword())).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.changePassword(1L, passwordDTO);

        // Assert
        verify(passwordEncoder).matches(passwordDTO.getCurrentPassword(), testUser.getPassword());
        verify(passwordEncoder).encode(passwordDTO.getNewPassword());
        verify(userRepository).save(any(User.class));
        assertEquals("encodedNewPassword", testUser.getPassword());
    }

    @Test
    void changePassword_WrongOldPassword() {
        // Arrange
        PasswordChangeDTO passwordDTO = new PasswordChangeDTO();
        passwordDTO.setCurrentPassword("wrongPassword");
        passwordDTO.setNewPassword("newPassword");
        passwordDTO.setConfirmPassword("newPassword");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(passwordDTO.getCurrentPassword(), testUser.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> userService.changePassword(1L, passwordDTO));
        verify(passwordEncoder).matches(passwordDTO.getCurrentPassword(), testUser.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_Success() {
        // Arrange
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setFirstName("Updated");
        updateDTO.setLastName("Name");
        updateDTO.setBio("New bio");
        updateDTO.setLocation("New location");
        updateDTO.setBirthDate(LocalDate.now());
        updateDTO.setProfession("New profession");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateUser(1L, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Updated", result.getFirstName());
        assertEquals("Name", result.getLastName());
        assertEquals("New bio", result.getBio());
        assertEquals("New location", result.getLocation());
        assertEquals(LocalDate.now(), result.getBirthDate());
        assertEquals("New profession", result.getProfession());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository).delete(testUser);
    }

    @Test
    void followUser_Success() {
        // Arrange
        User followedUser = new User();
        followedUser.setId(2L);
        followedUser.setUsername("followedUser");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(followedUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.followUser(1L, 2L);

        // Assert
        assertTrue(testUser.getFollowing().contains(followedUser));
        assertTrue(followedUser.getFollowers().contains(testUser));
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void unfollowUser_Success() {
        // Arrange
        User followedUser = new User();
        followedUser.setId(2L);
        followedUser.setUsername("followedUser");
        testUser.getFollowing().add(followedUser);
        followedUser.getFollowers().add(testUser);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(followedUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.unfollowUser(1L, 2L);

        // Assert
        assertFalse(testUser.getFollowing().contains(followedUser));
        assertFalse(followedUser.getFollowers().contains(testUser));
        verify(userRepository, times(2)).save(any(User.class));
    }
} 