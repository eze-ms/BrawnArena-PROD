package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.BuildService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.EntityResponse;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class BuildHandlerTest {
    @Mock private BuildService buildService;
    @Mock private JwtService jwtService;
    @Mock private ServerRequest request;


    @InjectMocks
    private BuildHandler buildHandler;

    @Test
    void startBuild_Success_ReturnsOkWithBuild() {
        // 1. Configuración con TUS campos reales
        String playerId = "player123";
        String characterId = "char456";
        Build mockBuild = new Build();
        mockBuild.setId("build789");
        mockBuild.setPlayerId(playerId);
        mockBuild.setCharacterId(characterId);
        mockBuild.setPiecesPlaced(new ArrayList<>());
        mockBuild.setErrors(0);
        mockBuild.setScore(100);

        // Mock principal (autenticación)
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");
        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        // Mock queryParam
        when(request.queryParam("characterId"))
                .thenReturn(Optional.of(characterId));

        // Mock del servicio
        when(buildService.startBuild(playerId, characterId))
                .thenReturn(Mono.just(mockBuild));

        // 2. Ejecución
        Mono<ServerResponse> response = buildHandler.startBuild(request);

        // 3. Validaciones con StepVerifier
        StepVerifier.create(response)
                .assertNext(res -> {
                    // Verifica status code
                    assertEquals(HttpStatus.OK, res.statusCode());

                    // Verifica el body (asumiendo que usas bodyValue)
                    Object body = ((EntityResponse) res).entity();
                    assertInstanceOf(Build.class, body);
                    assertEquals(mockBuild.getId(), ((Build) body).getId());

                    // Opcional: Verifica logs (si usas @Spy para el logger)
                })
                .verifyComplete();
    }

    @Test
    void startBuild_MissingCharacterId_ReturnsBadRequest() {
        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.queryParam("characterId")).thenReturn(Optional.empty());

        StepVerifier.create(buildHandler.startBuild(request))
                .expectErrorMatches(error ->
                        error instanceof UserNotFoundException &&
                                error.getMessage().contains("characterId"))
                .verify();
    }

    @Test
    void startBuild_ServiceFails_PropagatesError() {
        String playerId = "player123";
        String characterId = "char456";

        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");
        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.queryParam("characterId"))
                .thenReturn(Optional.of(characterId));
        when(buildService.startBuild(playerId, characterId))
                .thenReturn(Mono.error(new RuntimeException("DB failure")));

        StepVerifier.create(buildHandler.startBuild(request))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                error.getMessage().contains("DB failure"))
                .verify();
    }


    @Test
    void startBuild_MissingCharacterId_ReturnsBadRequestWithMessage() {
        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.queryParam("characterId")).thenReturn(Optional.empty());

        StepVerifier.create(buildHandler.startBuild(request))
                .expectErrorMatches(error ->
                        error instanceof UserNotFoundException &&
                                error.getMessage().contains("characterId"))
                .verify();
    }

    @Test
    void startBuild_ServiceError_ReturnsInternalServerError() {
        // 1. Configuración
        String playerId = "player123";
        String characterId = "char456";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.queryParam("characterId"))
                .thenReturn(Optional.of(characterId));

        when(buildService.startBuild(playerId, characterId))
                .thenReturn(Mono.error(new RuntimeException("Error en el servicio")));

        StepVerifier.create(buildHandler.startBuild(request))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                error.getMessage().contains("Error en el servicio"))
                .verify();
    }

    @Test
    void startBuild_MissingAuthentication_ThrowsUserNotFoundException() {
        when(request.principal())
                .thenAnswer(inv -> Mono.empty());

        StepVerifier.create(buildHandler.startBuild(request))
                .expectErrorMatches(error ->
                        error instanceof UserNotFoundException &&
                                error.getMessage().contains("Autenticación requerida"))
                .verify();
    }


    @Test
    void startBuild_WhenServiceFails_PropagatesInternalServerError() {
        // 1. Configuración
        String playerId = "player123";
        String characterId = "char456";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.queryParam("characterId"))
                .thenReturn(Optional.of(characterId));

        when(buildService.startBuild(playerId, characterId))
                .thenReturn(Mono.error(new RuntimeException("Error en el servicio")));

        // 2. Ejecución y validación
        StepVerifier.create(buildHandler.startBuild(request))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                error.getMessage().contains("Error en el servicio"))
                .verify();
    }

    @Test
    void startBuild_EmptyCharacterId_ReturnsBadRequest() {
        // 1. Configuración
        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.queryParam("characterId")).thenReturn(Optional.of(""));

        when(buildService.startBuild(playerId, "")).thenReturn(
                Mono.error(new IllegalArgumentException("Parámetro 'characterId' inválido"))
        );


        // 2. Ejecución y validación
        StepVerifier.create(buildHandler.startBuild(request))
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException &&
                                error.getMessage().contains("characterId"))
                .verify();
    }

    @Test
    void validateBuild_ValidBuild_ReturnsOkWithResult() {
        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        Build buildRequest = new Build();
        buildRequest.setCharacterId("char1");
        buildRequest.setPiecesPlaced(List.of("piezaCorrecta1", "piezaCorrecta2"));
        buildRequest.setDuration(60L);

        when(request.bodyToMono(Build.class)).thenReturn(Mono.just(buildRequest));

        Build buildValidado = new Build();
        buildValidado.setValid(true);
        buildValidado.setScore(100);

        when(buildService.validateBuild(playerId, buildRequest))
                .thenReturn(Mono.just(buildValidado));

        StepVerifier.create(buildHandler.validateBuild(request))
                .assertNext(res -> {
                    assertEquals(HttpStatus.OK, res.statusCode());

                    Object body = ((EntityResponse<?>) res).entity();
                    assertInstanceOf(Build.class, body);
                    Build result = (Build) body;
                    assertTrue(result.isValid());
                    assertEquals(100, result.getScore());
                })
                .verifyComplete();
    }

    @Test
    void validateBuild_InvalidBody_ReturnsBadRequestWithMessage() {
        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        Build buildInvalido = new Build();
        buildInvalido.setCharacterId(null);
        buildInvalido.setPiecesPlaced(null);
        buildInvalido.setDuration(60L);

        when(request.bodyToMono(Build.class)).thenReturn(Mono.just(buildInvalido));

        StepVerifier.create(buildHandler.validateBuild(request))
                .assertNext(res -> {
                    assertEquals(HttpStatus.BAD_REQUEST, res.statusCode());
                    Object body = ((EntityResponse<?>) res).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("Faltan datos obligatorios"));
                })
                .verifyComplete();
    }

    @Test
    void validateBuild_UsuarioNoAutenticado_LanzaUserNotFoundException() {
        when(request.principal()).thenReturn(Mono.empty());

        StepVerifier.create(buildHandler.validateBuild(request))
                .expectErrorMatches(error ->
                        error instanceof UserNotFoundException &&
                                error.getMessage().contains("Autenticación requerida"))
                .verify();
    }

    @Test
    void validateBuild_ErrorEnServicio_LanzaExcepcion() {
        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        Build buildRequest = new Build();
        buildRequest.setCharacterId("char1");
        buildRequest.setPiecesPlaced(List.of("pieza1"));
        buildRequest.setDuration(60L);

        when(request.bodyToMono(Build.class)).thenReturn(Mono.just(buildRequest));

        when(buildService.validateBuild(playerId, buildRequest))
                .thenReturn(Mono.error(new RuntimeException("Error interno en el servicio")));

        StepVerifier.create(buildHandler.validateBuild(request))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                error.getMessage().contains("Error interno"))
                .verify();
    }


}