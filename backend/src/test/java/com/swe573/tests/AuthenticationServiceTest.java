package com.swe573.tests;

import com.swe573.services.JwtService;
import com.swe573.services.UserService;
import com.swe573.services.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    private AuthenticationManager authenticationManager;

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
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(token);

        // Act
        String result = authenticationService.authenticate(username, password);

        // Assert
        assertNotNull(result);
        assertEquals(token, result);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void authenticate_UserNotFound() {
        // Arrange
        String username = "nonexistentUser";
        String password = "password123";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new RuntimeException("User not found"));

        // Act
        String result = authenticationService.authenticate(username, password);

        // Assert
        assertNull(result);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void authenticate_WrongPassword() {
        // Arrange
        String username = "testUser";
        String password = "wrongPassword";
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act
        String result = authenticationService.authenticate(username, password);

        // Assert
        assertNull(result);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void authenticate_EmptyCredentials() {
        // Act & Assert
        assertNull(authenticationService.authenticate("", "password"));
        assertNull(authenticationService.authenticate("username", ""));
        assertNull(authenticationService.authenticate("", ""));
        
        verify(authenticationManager, never()).authenticate(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void authenticate_NullCredentials() {
        // Act & Assert
        assertNull(authenticationService.authenticate(null, "password"));
        assertNull(authenticationService.authenticate("username", null));
        assertNull(authenticationService.authenticate(null, null));
        
        verify(authenticationManager, never()).authenticate(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).generateToken(any());
    }
} 