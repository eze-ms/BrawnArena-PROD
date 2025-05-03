package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.SharedModel;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.BuildRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.CharacterRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.SharedModelRepository;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@AutoConfigureWebTestClient
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityConfigIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CharacterRepository characterRepository;

    @MockBean
    private BuildRepository buildRepository;

    @MockBean
    private SharedModelRepository sharedModelRepository;

    @BeforeAll
    static void setupTestEnv() {
        System.setProperty("test.env", "true");
    }

    @BeforeEach
    void setupJwtMock() {
        Claims claims = Jwts.claims();
        claims.setSubject("player1");
        claims.put("role", "USER");

        when(jwtService.validateToken(anyString())).thenReturn(true);
        when(jwtService.getClaims(anyString())).thenReturn(claims);
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

    @Test
    void publicRouteShouldBeAccessibleWithoutAuthentication() {
        when(sharedModelRepository.findAll()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/gallery")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void postGalleryShareWithoutToken_shouldReturnUnauthorized() {
        webTestClient.post()
                .uri("/gallery/share")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void postGalleryShareWithValidToken_shouldReturnOk() {
        String fakeToken = "Bearer faketoken123";

        Claims claims = Jwts.claims();
        claims.setSubject("player1");
        claims.put("role", "USER");

        when(jwtService.validateToken(anyString())).thenReturn(true);
        when(jwtService.getClaims(anyString())).thenReturn(claims);

        User user = User.builder().nickname("player1").build();

        Character character = new Character(
                "char1",
                "Test-char1",
                "Descripción",
                "Medium",
                new ArrayList<>(),
                new ArrayList<>(),
                "image.png",
                0,
                "test-game.png"
        );

        Build build = new Build();
        build.setPlayerId("player1");
        build.setCharacterId("char1");
        build.setValid(true);
        build.setScore(90);
        build.setCreatedAt(Instant.now());

        SharedModel sharedModel = new SharedModel();
        sharedModel.setPlayerId("player1");
        sharedModel.setCharacterId("char1");
        sharedModel.setScore(90);
        sharedModel.setPowers(List.of());
        sharedModel.setSharedAt(Instant.now());

        // Mocks de repositorios
        when(userRepository.findByNickname("player1")).thenReturn(Mono.just(user));
        when(characterRepository.findById("char1")).thenReturn(Mono.just(character));
        when(buildRepository.findByPlayerIdAndCharacterIdAndValidTrue("player1", "char1"))
                .thenReturn(Flux.just(build));
        when(sharedModelRepository.save(any(SharedModel.class)))
                .thenReturn(Mono.just(sharedModel));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/gallery/share")
                        .queryParam("characterId", "char1")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, fakeToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(result -> System.out.println(new String(result.getResponseBody())));

    }

    @Test
    void putGalleryHighlightedWithUserRole_shouldReturnForbidden() {
        String fakeToken = "Bearer faketoken123";

        Claims claims = Jwts.claims();
        claims.setSubject("player1");
        claims.put("role", "USER");

        when(jwtService.validateToken(anyString())).thenReturn(true);
        when(jwtService.getClaims(anyString())).thenReturn(claims);

        webTestClient.put()
                .uri("/gallery/highlighted")
                .header(HttpHeaders.AUTHORIZATION, fakeToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void putGalleryHighlightedWithAdminRole_shouldReturnOk() {
        String fakeToken = "Bearer faketoken123";

        Claims claims = Jwts.claims();
        claims.setSubject("adminUser");
        claims.put("role", "ADMIN");

        when(jwtService.validateToken(anyString())).thenReturn(true);
        when(jwtService.getClaims(anyString())).thenReturn(claims);

        // Mock necesario para evitar error interno (mínimo viable)
        SharedModel destacado = new SharedModel();
        destacado.setId("model123");
        destacado.setHighlighted(true);

        when(sharedModelRepository.findById("model123")).thenReturn(Mono.just(destacado));
        when(sharedModelRepository.findByHighlightedTrue()).thenReturn(Flux.empty());
        when(sharedModelRepository.save(any(SharedModel.class))).thenReturn(Mono.just(destacado));

        webTestClient.put()
                .uri("/gallery/highlighted")
                .header(HttpHeaders.AUTHORIZATION, fakeToken)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("model123")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void deleteGalleryWithoutToken_shouldReturnUnauthorized() {
        webTestClient.delete()
                .uri("/gallery/model123")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deleteGalleryWithValidToken_shouldReturnOk() {
        String fakeToken = "Bearer faketoken123";

        Claims claims = Jwts.claims();
        claims.setSubject("player1");
        claims.put("role", "USER");

        when(jwtService.validateToken(anyString())).thenReturn(true);
        when(jwtService.getClaims(anyString())).thenReturn(claims);

        SharedModel model = new SharedModel();
        model.setId("model123");
        model.setPlayerId("player1");

        when(sharedModelRepository.findById("model123")).thenReturn(Mono.just(model));
        when(sharedModelRepository.delete(model)).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/gallery/model123")
                .header(HttpHeaders.AUTHORIZATION, fakeToken)
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    void getUserMeWithoutToken_shouldReturnUnauthorized() {
        webTestClient.get()
                .uri("/users/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getUserMeWithValidToken_shouldReturnOk() {
        String fakeToken = "Bearer faketoken123";

        Claims claims = Jwts.claims();
        claims.setSubject("player1");
        claims.put("role", "USER");

        when(jwtService.validateToken(anyString())).thenReturn(true);
        when(jwtService.getClaims(anyString())).thenReturn(claims);

        // Mock mínimo para que el endpoint responda sin error interno
        User user = User.builder().nickname("player1").build();
        when(userRepository.findByNickname("player1")).thenReturn(Mono.just(user));

        webTestClient.get()
                .uri("/users/me")
                .header(HttpHeaders.AUTHORIZATION, fakeToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void requestWithTokenWithoutBearerPrefix_shouldReturnUnauthorized() {
        String invalidHeader = "faketoken123";

        webTestClient.get()
                .uri("/users/me")
                .header(HttpHeaders.AUTHORIZATION, invalidHeader)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void requestWithInvalidBearerToken_shouldReturnUnauthorized() {
        String invalidToken = "Bearer invalid.token.value";

        when(jwtService.validateToken(anyString())).thenReturn(false);

        webTestClient.get()
                .uri("/users/me")
                .header(HttpHeaders.AUTHORIZATION, invalidToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void putGalleryHighlightedWithAdminRole_shouldBeAuthorized() {
        String fakeToken = "Bearer faketoken123";

        Claims claims = Jwts.claims();
        claims.setSubject("adminUser");
        claims.put("role", "ADMIN");

        when(jwtService.validateToken(anyString())).thenReturn(true);
        when(jwtService.getClaims(anyString())).thenReturn(claims);

        SharedModel model = new SharedModel();
        model.setId("model123");
        model.setHighlighted(false);

        when(sharedModelRepository.findById("model123")).thenReturn(Mono.just(model));
        when(sharedModelRepository.findByHighlightedTrue()).thenReturn(Flux.empty());
        when(sharedModelRepository.save(any(SharedModel.class))).thenReturn(Mono.just(model));

        webTestClient.put()
                .uri("/gallery/highlighted")
                .header(HttpHeaders.AUTHORIZATION, fakeToken)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("model123")
                .exchange()
                .expectStatus().isOk();
    }

}

