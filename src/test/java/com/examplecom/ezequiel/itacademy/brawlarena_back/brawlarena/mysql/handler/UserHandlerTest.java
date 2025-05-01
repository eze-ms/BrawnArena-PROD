package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.handler;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers.GalleryHandler;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.server.EntityResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.test.StepVerifier;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserHandlerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserHandler userHandler;

    @InjectMocks
    private GalleryHandler galleryHandler;

    @Mock
    private ServerRequest request;

    // Método auxiliar único para los tests
    private ServerRequest createMockRequest(String username, Object requestBody) {
        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn(username);

        ServerRequest request = Mockito.mock(ServerRequest.class);
        Mockito.doReturn(Mono.just(auth)).when(request).principal();

        if (requestBody != null) {
            if (requestBody instanceof User) {
                when(request.bodyToMono(User.class)).thenReturn(Mono.just((User) requestBody));
            } else if (requestBody instanceof Integer) {
                when(request.bodyToMono(Integer.class)).thenReturn(Mono.just((Integer) requestBody));
            } else if (requestBody instanceof Long) {
                when(request.bodyToMono(Long.class)).thenReturn(Mono.just((Long) requestBody));
            }
        }
        return request;
    }

    @Test
    void getCurrentUser_Success() {
        String nickname = "testUser";
        User mockUser = new User(1L, nickname, "pass", 100, "USER", "[]");
        ServerRequest request = createMockRequest(nickname, null);

        when(userService.findByNickname(nickname)).thenReturn(Mono.just(mockUser));

        Mono<ServerResponse> response = userHandler.getCurrentUser(request);

        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    // Usamos assertEquals para verificar el código de estado
                    assertEquals(HttpStatus.OK, serverResponse.statusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getCurrentUser_UserNotFound() {

        String nickname = "nonExistentUser";

        ServerRequest request = createMockRequest(nickname, null);

        when(userService.findByNickname(nickname))
                .thenReturn(Mono.error(new UserNotFoundException("Usuario no encontrado")));

        Mono<ServerResponse> response = userHandler.getCurrentUser(request);

        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(HttpStatus.NOT_FOUND, serverResponse.statusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void updateUserTokens_Success() {

        String nickname = "testUser";
        int newTokens = 150;
        User updatedUser = new User(1L, nickname, "password123", newTokens, "USER", "[]");

        ServerRequest request = createMockRequest(nickname, newTokens);
        when(userService.updateTokens(nickname, newTokens)).thenReturn(Mono.just(updatedUser));

        StepVerifier.create(userHandler.updateUserTokens(request))
                .expectNextMatches(serverResponse -> {
                    assertEquals(HttpStatus.OK, serverResponse.statusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void updateUserTokens_InvalidInput() {

        String nickname = "testUser";
        int invalidTokens = -50;

        ServerRequest request = createMockRequest(nickname, invalidTokens);
        when(userService.updateTokens(nickname, invalidTokens))
                .thenReturn(Mono.error(new IllegalArgumentException("Tokens no pueden ser negativos")));

        StepVerifier.create(userHandler.updateUserTokens(request))
                .expectNextMatches(serverResponse -> {
                    assertEquals(HttpStatus.BAD_REQUEST, serverResponse.statusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getUserGallery_AuthenticatedUser_ReturnsOkWithIds() {
        String playerId = "player123";
        List<Long> characterIds = List.of(1L, 2L);

        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(userService.getCharacterIds(playerId))
                .thenReturn(Mono.just(characterIds));

        StepVerifier.create(userHandler.getUserGallery(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(List.class, body);

                    @SuppressWarnings("unchecked")
                    List<Long> result = (List<Long>) body;

                    assertEquals(2, result.size());
                    assertEquals(1L, result.get(0));
                    assertEquals(2L, result.get(1));
                })
                .verifyComplete();
    }

    @Test
    void getUserGallery_MissingAuthentication_ReturnsBadRequest() {
        when(request.principal())
                .thenReturn(Mono.empty());

        StepVerifier.create(userHandler.getUserGallery(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("Autenticación requerida"));
                })
                .verifyComplete();
    }

    @Test
    void getUserGallery_ServiceError_ReturnsBadRequest() {
        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(userService.getCharacterIds(playerId))
                .thenReturn(Mono.error(new RuntimeException("Fallo en el servicio")));

        StepVerifier.create(userHandler.getUserGallery(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("Fallo en el servicio"));
                })
                .verifyComplete();
    }

    @Test
    void addCharacterId_Success() throws JsonProcessingException {

        String nickname = "testUser";
        Long characterId = 123L;

        String characterIdsJson = new ObjectMapper()
                .writeValueAsString(new ArrayList<>(List.of(characterId)));

        User mockUser = User.builder()
                .nickname(nickname)
                .characterIds(characterIdsJson)
                .build();


        System.out.println("[TEST] User mockeado: " + mockUser);

        ServerRequest request = createMockRequest(nickname, characterId);

        request.bodyToMono(Long.class).subscribe(b ->
                System.out.println("[TEST] Body del request mockeado: " + b));
        request.principal().subscribe(p ->
                System.out.println("[TEST] Principal mockeado: " + p));

        when(userService.addCharacterId(nickname, characterId))
                .thenReturn(Mono.just(mockUser));

        StepVerifier.create(userHandler.addCharacterId(request))
                .expectNextMatches(res -> {
                    System.out.println("[TEST] Respuesta recibida: " + res.statusCode());
                    return res.statusCode() == HttpStatus.OK;
                })
                .verifyComplete();
    }

    @Test
    void addCharacterId_UserNotFound() {

        String nickname = "usuarioInexistente";
        Long characterId = 123L;

        ServerRequest request = createMockRequest(nickname, characterId);
        when(userService.addCharacterId(nickname, characterId))
                .thenReturn(Mono.error(new UserNotFoundException("Usuario no encontrado")));

        StepVerifier.create(userHandler.addCharacterId(request))
                .expectNextMatches(res -> {
                    System.out.println("[TEST] Status de error recibido: " + res.statusCode());
                    return res.statusCode() == HttpStatus.BAD_REQUEST;
                })
                .verifyComplete();
    }
}