package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.common.constant.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "key", Keys.secretKeyFor(SignatureAlgorithm.HS256));
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L); // 1 hora de validez
    }

    @Test
    void generateToken_ReturnsValidTokenWithUserRole() {
        String token = jwtService.generateToken("user1", Role.USER);
        Claims claims = jwtService.getClaims(token);

        assertNotNull(token);
        assertEquals("user1", claims.getSubject());
        assertEquals("USER", claims.get("role", String.class));
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void generateToken_ReturnsValidTokenWithAdminRole() {
        String token = jwtService.generateToken("admin1", Role.ADMIN);
        Claims claims = jwtService.getClaims(token);

        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void validateToken_ReturnsTrueForValidToken() {
        String token = jwtService.generateToken("user1", Role.USER);

        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void validateToken_ReturnsFalseForInvalidToken() {
        String invalidToken = "token.invalido.123";

        assertFalse(jwtService.validateToken(invalidToken));
    }

    @Test
    void validateToken_ReturnsFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -3600000L);
        String expiredToken = jwtService.generateToken("user1", Role.USER);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);

        assertFalse(jwtService.validateToken(expiredToken));
    }

    @Test
    void getClaims_ReturnsCorrectClaimsForValidToken() {
        String token = jwtService.generateToken("user1", Role.USER);

        Claims claims = jwtService.getClaims(token);

        assertEquals("user1", claims.getSubject());
        assertEquals("USER", claims.get("role", String.class));
        assertNotNull(claims.getExpiration());
    }

    @Test
    void getClaims_ThrowsExceptionForInvalidToken() {
        String invalidToken = "token.invalido.123";

        assertThrows(JwtException.class, () -> jwtService.getClaims(invalidToken));
    }
}