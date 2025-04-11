package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.handler;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.common.constant.Role;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.test.StepVerifier;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserHandlerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserHandler userHandler;

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

    //! test getCurrentUser
    @Test
    void getCurrentUser_Success() {
        String nickname = "testUser";
        User mockUser = new User(1L, nickname, "pass", 100, Role.USER, List.of());
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

        // Usa el método helper para crear el mock
        ServerRequest request = createMockRequest(nickname, null);

        // Configura el mock del servicio para devolver error
        when(userService.findByNickname(nickname))
                .thenReturn(Mono.error(new UserNotFoundException("Usuario no encontrado")));

        Mono<ServerResponse> response = userHandler.getCurrentUser(request);

        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(HttpStatus.NOT_FOUND, serverResponse.statusCode()); // Usamos assertEquals aquí
                    return true;
                })
                .verifyComplete();
    }

    //! test updateUserTokens
    @Test
    void updateUserTokens_Success() {
        String nickname = "testUser";
        int newTokens = 150;
        User updatedUser = new User(1L, nickname, "password123", newTokens, Role.USER, List.of());

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

    //! test addCharacterId
    @Test
    void addCharacterId_Success() {
        String nickname = "testUser";
        Long characterId = 123L;

        User mockUser = User.builder()
                .nickname(nickname)
                .characterIds(new ArrayList<>(List.of(characterId))) 
                .build();

        // Debug del mock
        System.out.println("[TEST] User mockeado: " + mockUser);

        ServerRequest request = createMockRequest(nickname, characterId);

        // Debug del request mockeado
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
        // Arrange
        String nickname = "usuarioInexistente";
        Long characterId = 123L;

        ServerRequest request = createMockRequest(nickname, characterId);

        // Mock del servicio para simular que el usuario no existe
        when(userService.addCharacterId(nickname, characterId))
                .thenReturn(Mono.error(new UserNotFoundException("Usuario no encontrado")));

        // Act & Assert
        StepVerifier.create(userHandler.addCharacterId(request))
                .expectNextMatches(res -> {
                    System.out.println("[TEST] Status de error recibido: " + res.statusCode());
                    return res.statusCode() == HttpStatus.BAD_REQUEST; // Verifica que devuelve 400
                })
                .verifyComplete();
    }

}