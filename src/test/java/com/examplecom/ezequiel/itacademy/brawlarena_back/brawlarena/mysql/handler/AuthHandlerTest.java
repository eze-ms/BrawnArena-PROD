package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.handler;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.common.constant.Role;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.NicknameAlreadyExistsException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.dto.LoginRequest;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.service.UserService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.CharacterRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.netty.handler.codec.Headers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.server.EntityResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CharacterRepository characterRepository;

    @Mock
    private Headers headers;

    @Mock
    private ServerRequest request;

    @InjectMocks
    private AuthHandler authHandler;

    @Test
    void registerUser_Success() {
        User newUser = new User(null, "newUser", "password123", 100, "USER", "[]");

        Character freeCharacter = new Character();
        freeCharacter.setId("free1");
        freeCharacter.setCost(0);

        when(request.bodyToMono(User.class)).thenReturn(Mono.just(newUser));
        when(characterRepository.findAll()).thenReturn(Flux.just(freeCharacter));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userService.save(userCaptor.capture())).thenAnswer(invocation -> Mono.just(userCaptor.getValue()));

        StepVerifier.create(authHandler.registerUser(request))
                .expectNextMatches(response -> response.statusCode() == HttpStatus.CREATED)
                .verifyComplete();

        // Verificación explícita del campo characterIds
        User captured = userCaptor.getValue();
        assertNotNull(captured);
        assertEquals("[\"free1\"]", captured.getCharacterIds());
    }

    @Test
    void registerUser_ConflictWhenNicknameExists() {
        User existingUser = new User(null, "existingUser", "password123", 100, "USER", "[]");

        when(request.bodyToMono(User.class))
                .thenReturn(Mono.just(existingUser));

        when(characterRepository.findAll())
                .thenReturn(Flux.empty());

        when(userService.save(any(User.class)))
                .thenReturn(Mono.error(new NicknameAlreadyExistsException("Nickname already exists")));

        StepVerifier.create(authHandler.registerUser(request))
                .expectNextMatches(serverResponse -> serverResponse.statusCode() == HttpStatus.CONFLICT)
                .verifyComplete();
    }

    @Test
    void loginUser_Success() {

        LoginRequest loginRequest = new LoginRequest("user1", "pass123");
        User mockUser = new User(1L, "user1", "hashedPass", 100, "USER", "[]");

        when(request.bodyToMono(LoginRequest.class)).thenReturn(Mono.just(loginRequest));
        when(userService.findByNickname("user1")).thenReturn(Mono.just(mockUser));
        when(passwordEncoder.matches("pass123", "hashedPass")).thenReturn(true);
        when(jwtService.generateToken("user1", Role.USER)).thenReturn("mockToken");

        StepVerifier.create(authHandler.loginUser(request))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void loginUser_InvalidPassword() {
        LoginRequest loginRequest = new LoginRequest("user1", "wrongPass");
        User mockUser = new User(1L, "user1", "hashedPass", 100, "USER", "[]");
        when(request.bodyToMono(LoginRequest.class)).thenReturn(Mono.just(loginRequest));
        when(userService.findByNickname("user1")).thenReturn(Mono.just(mockUser));
        when(passwordEncoder.matches("wrongPass", "hashedPass")).thenReturn(false);

        StepVerifier.create(authHandler.loginUser(request))
                .expectNextMatches(res -> res.statusCode() == HttpStatus.UNAUTHORIZED)
                .verifyComplete();
    }

    @Test
    void loginUser_UserNotFound() {
        LoginRequest loginRequest = new LoginRequest("unknown", "pass");
        when(request.bodyToMono(LoginRequest.class)).thenReturn(Mono.just(loginRequest));
        when(userService.findByNickname("unknown")).thenReturn(Mono.empty());

        StepVerifier.create(authHandler.loginUser(request))
                .expectNextMatches(res -> res.statusCode() == HttpStatus.UNAUTHORIZED)
                .verifyComplete();
    }

    @Test
    void validateToken_ValidToken() {

        ServerRequest.Headers headersMock = Mockito.mock(ServerRequest.Headers.class);
        when(request.headers()).thenReturn(headersMock);
        when(headersMock.firstHeader("Authorization")).thenReturn("Bearer validToken");

        Claims mockClaims = Jwts.claims().setSubject("user1");
        mockClaims.put("role", "USER");
        when(jwtService.validateToken("validToken")).thenReturn(true);
        when(jwtService.getClaims("validToken")).thenReturn(mockClaims);

        StepVerifier.create(authHandler.validateToken(request))
                .expectNextMatches(res -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) ((EntityResponse<?>) res).entity();
                    return res.statusCode() == HttpStatus.OK &&
                            "user1".equals(body.get("nickname")) &&
                            "USER".equals(body.get("role")) &&
                            Boolean.TRUE.equals(body.get("valid"));
                })
                .verifyComplete();
    }

    @Test
    void validateToken_InvalidToken() {

        ServerRequest.Headers headersMock = Mockito.mock(ServerRequest.Headers.class);
        when(request.headers()).thenReturn(headersMock);
        when(headersMock.firstHeader("Authorization")).thenReturn("Bearer invalidToken");

        when(jwtService.validateToken("invalidToken")).thenReturn(false);

        StepVerifier.create(authHandler.validateToken(request))
                .expectNextMatches(res -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) ((EntityResponse<?>) res).entity();
                    return res.statusCode() == HttpStatus.OK &&
                            Boolean.FALSE.equals(body.get("valid"));
                })
                .verifyComplete();
    }

    @Test
    void validateToken_MissingToken() {

        ServerRequest.Headers headersMock = Mockito.mock(ServerRequest.Headers.class);
        when(request.headers()).thenReturn(headersMock);
        when(headersMock.firstHeader("Authorization")).thenReturn(null);

        StepVerifier.create(authHandler.validateToken(request))
                .expectNextMatches(res -> res.statusCode() == HttpStatus.UNAUTHORIZED)
                .verifyComplete();
    }
}