package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void setupTestEnv() {
        System.setProperty("test.env", "true");
    }

    @Test
    void passwordEncoder_shouldReturnBCryptEncoder() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder encoder = config.passwordEncoder();
        assertNotNull(encoder);
        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
    }

    @Test
    void protectedRouteShouldReturnUnauthorizedWithoutToken() {
        webTestClient.get()
                .uri("/users/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

}
