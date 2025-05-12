package com.swe573.tests;

import com.swe573.models.User;
import com.swe573.models.enums.Role;
import com.swe573.repositories.UserRepository;
import com.swe573.services.impl.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.USER);
    }

    @Test
    void loadUserByUsername_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("password", userDetails.getPassword());
        
        // Check authorities
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));
        
        assertEquals("ROLE_USER", authorities);
        
        // Verify repository method was called
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_UserNotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistentuser")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("nonexistentuser");
        });
        
        assertTrue(exception.getMessage().contains("User not found with username: nonexistentuser"));
        verify(userRepository).findByUsername("nonexistentuser");
    }

    @Test
    void loadUserByUsername_AdminRole() {
        // Arrange
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("adminuser");
        adminUser.setPassword("adminpassword");
        adminUser.setRole(Role.ADMIN);
        
        when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(adminUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("adminuser");

        // Assert
        assertEquals("adminuser", userDetails.getUsername());
        
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));
        
        assertEquals("ROLE_ADMIN", authorities);
        verify(userRepository).findByUsername("adminuser");
    }
} 