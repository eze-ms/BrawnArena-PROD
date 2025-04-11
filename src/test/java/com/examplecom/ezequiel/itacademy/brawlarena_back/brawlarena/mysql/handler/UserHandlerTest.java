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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserHandlerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserHandler userHandler;

    // Método auxiliar único para ambos tests
    private ServerRequest createMockRequest(String username, User userToReturn) {
        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn(username);

        // Crea un mock del ServerRequest
        ServerRequest request = Mockito.mock(ServerRequest.class);

        // Si necesitas el cuerpo de la solicitud (en caso de 'registerUser')
        if (userToReturn != null) {
            when(request.bodyToMono(User.class)).thenReturn(Mono.just(userToReturn));
        }

        // Configura el principal (usuario autenticado)
        Mockito.doReturn(Mono.just(auth)).when(request).principal();

        return request;
    }

    //! test de getCurrentUser
    @Test
    void getCurrentUser_Success() {
        String nickname = "testUser";
        User mockUser = new User(1L, nickname, "password123", 100, Role.USER);

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
}


