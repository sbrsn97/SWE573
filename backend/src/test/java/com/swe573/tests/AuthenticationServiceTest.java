package com.swe573.tests;

import com.swe573.services.JwtService;
import com.swe573.services.UserService;
import com.swe573.services.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    void authenticate_Success() {
        // Arrange
        String username = "testUser";
        String password = "password123";
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            username, 
            password, 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = "test.jwt.token";

        when(userService.authenticateUser(eq(username), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(token);

        // Act
        String result = authenticationService.authenticate(username, password);

        // Assert
        assertNotNull(result);
        assertEquals(token, result);
        verify(userService).authenticateUser(eq(username), any());
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void authenticate_UserNotFound() {
        // Arrange
        String username = "nonexistentUser";
        String password = "password123";

        when(userService.authenticateUser(eq(username), any())).thenReturn(false);

        // Act
        String result = authenticationService.authenticate(username, password);

        // Assert
        assertNull(result);
        verify(userService).authenticateUser(eq(username), any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void authenticate_WrongPassword() {
        // Arrange
        String username = "testUser";
        String password = "wrongPassword";

        when(userService.authenticateUser(eq(username), any())).thenReturn(false);

        // Act
        String result = authenticationService.authenticate(username, password);

        // Assert
        assertNull(result);
        verify(userService).authenticateUser(eq(username), any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void authenticate_EmptyCredentials() {
        // Act & Assert
        assertNull(authenticationService.authenticate("", "password"));
        assertNull(authenticationService.authenticate("username", ""));
        assertNull(authenticationService.authenticate("", ""));
        
        verify(userService, never()).authenticateUser(any(), any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void authenticate_NullCredentials() {
        // Act & Assert
        assertNull(authenticationService.authenticate(null, "password"));
        assertNull(authenticationService.authenticate("username", null));
        assertNull(authenticationService.authenticate(null, null));
        
        verify(userService, never()).authenticateUser(any(), any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).generateToken(any());
    }
} 