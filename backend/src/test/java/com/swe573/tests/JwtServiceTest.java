package com.swe573.tests;

import com.swe573.services.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    private final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long JWT_EXPIRATION = 86400000; // 1 day in milliseconds
    private final long INACTIVITY_EXPIRATION = 3600000; // 1 hour in milliseconds

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", JWT_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "inactivityExpiration", INACTIVITY_EXPIRATION);
        
        when(userDetails.getUsername()).thenReturn("testuser");
    }

    @Test
    void extractUsername_Success() {
        // Generate a token using the service to ensure signatures match
        String token = jwtService.generateToken(userDetails);
        
        // Act
        String username = jwtService.extractUsername(token);
        
        // Assert
        assertEquals("testuser", username);
    }

    @Test
    void generateToken_Success() {
        // Act
        String token = jwtService.generateToken(userDetails);
        
        // Assert
        assertNotNull(token);
        String username = jwtService.extractUsername(token);
        assertEquals("testuser", username);
    }

    @Test
    void generateTokenWithClaims_Success() {
        // Arrange
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "USER");
        
        // Act
        String token = jwtService.generateToken(extraClaims, userDetails);
        
        // Assert
        assertNotNull(token);
        String username = jwtService.extractUsername(token);
        assertEquals("testuser", username);
    }

    @Test
    void isTokenValid_ValidToken() {
        // Arrange
        String token = jwtService.generateToken(userDetails);
        
        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);
        
        // Assert
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_ExpiredToken() throws Exception {
        // Create a token that's already expired using our own method
        // to ensure it can be parsed by the service
        String token = createExpiredToken();
        
        // For expired tokens, an ExpiredJwtException is thrown when extracting the username
        // which is part of the isTokenValid method flow. This should be handled as invalid.
        boolean isValid = false;
        try {
            isValid = jwtService.isTokenValid(token, userDetails);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // This is expected - an expired token should be considered invalid
            isValid = false;
        }
        
        // Assert
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_InvalidUsername() {
        // Arrange
        String token = jwtService.generateToken(userDetails);
        UserDetails anotherUser = mock(UserDetails.class);
        when(anotherUser.getUsername()).thenReturn("anotheruser");
        
        // Act
        boolean isValid = jwtService.isTokenValid(token, anotherUser);
        
        // Assert
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_InactiveToken() throws Exception {
        // Create a token with last activity longer than inactivity expiration
        String token = createInactiveToken();
        
        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);
        
        // Assert
        assertFalse(isValid);
    }

    @Test
    void extractClaim_Success() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        String token = jwtService.generateToken(claims, userDetails);
        
        // Act
        String role = jwtService.extractClaim(token, claims1 -> claims1.get("role", String.class));
        
        // Assert
        assertEquals("USER", role);
    }

    // Helper methods for creating test tokens using the same key as the service
    private String createExpiredToken() {
        return Jwts
                .builder()
                .setClaims(new HashMap<>())
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // 1 hour ago
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60)) // Expired 1 minute ago
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String createInactiveToken() {
        Map<String, Object> claims = new HashMap<>();
        // Set lastActivity to a time that exceeds the inactivity expiration
        claims.put("lastActivity", System.currentTimeMillis() - (INACTIVITY_EXPIRATION + 1000));
        
        return Jwts
                .builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignInKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
} 