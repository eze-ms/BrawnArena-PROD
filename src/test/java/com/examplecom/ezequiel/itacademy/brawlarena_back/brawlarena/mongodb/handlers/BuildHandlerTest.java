package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.NoPendingBuildException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.BuildService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.EntityResponse;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class BuildHandlerTest {
    @Mock
    private BuildService buildService;

    @Mock
    private ServerRequest request;

    @InjectMocks
    private BuildHandler buildHandler;

    @Test
    void startBuild_Success_ReturnsOkWithBuild() {

        String playerId = "player123";
        String characterId = "char456";
        Build mockBuild = new Build();
        mockBuild.setId("build789");
        mockBuild.setPlayerId(playerId);
        mockBuild.setCharacterId(characterId);
        mockBuild.setPiecesPlaced(new ArrayList<>());
        mockBuild.setErrors(0);
        mockBuild.setScore(100);

        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");
        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.queryParam("characterId"))
                .thenReturn(Optional.of(characterId));

        when(buildService.startBuild(playerId, characterId))
                .thenReturn(Mono.just(mockBuild));

        Mono<ServerResponse> response = buildHandler.startBuild(request);

        StepVerifier.create(response)
                .assertNext(res -> {
                    assertEquals(HttpStatus.OK, res.statusCode());

                    Object body = ((EntityResponse<?>) res).entity();
                    assertInstanceOf(Build.class, body);
                    assertEquals(mockBuild.getId(), ((Build) body).getId());

                })
                .verifyComplete();
    }

    @Test
    void startBuild_MissingCharacterId_ReturnsBadRequest() {

        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.queryParam("characterId"))
                .thenReturn(Optional.empty());

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

        when(request.queryParam("characterId"))
                .thenReturn(Optional.empty());

        StepVerifier.create(buildHandler.startBuild(request))
                .expectErrorMatches(error ->
                        error instanceof UserNotFoundException &&
                                error.getMessage().contains("characterId"))
                .verify();
    }

    @Test
    void startBuild_ServiceError_ReturnsInternalServerError() {

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
    void startBuild_EmptyCharacterId_ReturnsBadRequest() {

        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.queryParam("characterId"))
                .thenReturn(Optional.of(""));

        when(buildService.startBuild(playerId, ""))
                .thenReturn(
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
                    assertTrue(((String) body).contains("El campo characterId es obligatorio"));
                })
                .verifyComplete();
    }

    @Test
    void validateBuild_UsuarioNoAutenticado_LanzaUserNotFoundException() {

        when(request.principal())
                .thenReturn(Mono.empty());

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

        when(request.bodyToMono(Build.class))
                .thenReturn(Mono.just(buildRequest));

        when(buildService.validateBuild(playerId, buildRequest))
                .thenReturn(Mono.error(new RuntimeException("Error interno en el servicio")));

        StepVerifier.create(buildHandler.validateBuild(request))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                error.getMessage().contains("Error interno"))
                .verify();
    }

    @Test
    void getBuildHistory_AutenticacionFallida_LanzaUserNotFoundException() {

        when(request.principal())
                .thenReturn(Mono.empty());

        StepVerifier.create(buildHandler.getBuildHistory(request))
                .expectErrorMatches(error ->
                        error instanceof UserNotFoundException &&
                                error.getMessage().contains("Autenticación requerida"))
                .verify();
    }

    @Test
    void getBuildHistory_HistorialVacio_RetornaNoContent() {

        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(buildService.getBuildHistory(playerId))
                .thenReturn(Flux.empty());

        StepVerifier.create(buildHandler.getBuildHistory(request))
                .assertNext(res -> assertEquals(HttpStatus.NO_CONTENT, res.statusCode()))
                .verifyComplete();
    }

    @Test
    void getBuildHistory_ConDatos_RetornaOkConBuilds() {

        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");
        Build build1 = new Build();
        Build build2 = new Build();

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(buildService.getBuildHistory(playerId))
                .thenReturn(Flux.just(build1, build2));

        StepVerifier.create(buildHandler.getBuildHistory(request))
                .assertNext(res -> {
                    assertEquals(HttpStatus.OK, res.statusCode());
                })
                .verifyComplete();
    }

    @Test
    void getBuildHistory_ErrorEnServicio_PropagaError() {

        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(buildService.getBuildHistory(playerId))
                .thenReturn(Flux.error(new RuntimeException("Error de DB")));

        StepVerifier.create(buildHandler.getBuildHistory(request))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                error.getMessage().contains("Error de DB"))
                .verify();
    }

    @Test
    void getBuildHistory_AutenticacionExitosaPeroServicioFalla_PropagaError() {

        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(buildService.getBuildHistory(playerId))
                .thenReturn(Flux.error(new RuntimeException("Error en base de datos")));

        StepVerifier.create(buildHandler.getBuildHistory(request))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                error.getMessage().contains("Error en base de datos"))
                .verify();
    }

    @Test
    void getBuildHistory_ConBuilds_RetornaListaFormateada() {

        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        Build buildValido = new Build();
        buildValido.setId("build1");
        buildValido.setValid(true);

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(buildService.getBuildHistory(playerId))
                .thenReturn(Flux.just(buildValido));


        StepVerifier.create(buildHandler.getBuildHistory(request))
                .assertNext(res -> {
                    assertEquals(HttpStatus.OK, res.statusCode());
                    assertInstanceOf(EntityResponse.class, res);
                })
                .verifyComplete();
    }

    @Test
    void getBuildHistory_RetornaListaDeBuilds() {

        String playerId = "player123";
        Build build1 = new Build();
        build1.setId("build1");
        build1.setValid(true);

        Build build2 = new Build();
        build2.setId("build2");
        build2.setValid(true);

        when(request.principal())
                .thenAnswer(inv -> Mono.just(new UsernamePasswordAuthenticationToken(playerId, "")));
        when(buildService.getBuildHistory(playerId))
                .thenReturn(Flux.just(build1, build2));

        StepVerifier.create(buildHandler.getBuildHistory(request))
                .assertNext(res -> {
                    assertEquals(HttpStatus.OK, res.statusCode());

                    @SuppressWarnings("unchecked")
                    List<Build> builds = (List<Build>) ((EntityResponse<?>) res).entity();

                    assertEquals(2, builds.size());
                    assertEquals("build1", builds.get(0).getId());
                    assertEquals("build2", builds.get(1).getId());
                })
                .verifyComplete();
    }

    @Test
    void getPendingBuild_MissingCharacterId_ReturnsBadRequest() {

        when(request.queryParam("characterId"))
                .thenReturn(Optional.empty());

        StepVerifier.create(buildHandler.getPendingBuild(request))
                .assertNext(res -> assertEquals(HttpStatus.BAD_REQUEST, res.statusCode()))
                .verifyComplete();
    }

    @Test
    void getPendingBuild_ExistingBuild_ReturnsOkWithBuild() {

        String playerId = "player123";
        String characterId = "char456";
        Build mockBuild = new Build();
        mockBuild.setId("build789");
        mockBuild.setPlayerId(playerId);
        mockBuild.setCharacterId(characterId);
        mockBuild.setValid(false);

        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");
        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));
        when(request.queryParam("characterId"))
                .thenReturn(Optional.of(characterId));
        when(buildService.getPendingBuild(playerId, characterId))
                .thenReturn(Mono.just(mockBuild));

        StepVerifier.create(buildHandler.getPendingBuild(request))
                .assertNext(res -> {
                    assertEquals(HttpStatus.OK, res.statusCode());
                    Object body = ((EntityResponse<?>) res).entity();
                    assertInstanceOf(Build.class, body);
                    Build responseBuild = (Build) body;
                    assertEquals("build789", responseBuild.getId());
                    assertFalse(responseBuild.isValid());
                })
                .verifyComplete();
    }

    @Test
    void getPendingBuild_NoExisteBuild_RetornaNotFound() {

        String playerId = "player1";
        String characterId = "char123";

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(playerId);
        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.queryParam("characterId"))
                .thenReturn(Optional.of(characterId));

        when(buildService.getPendingBuild(playerId, characterId))
                .thenReturn(Mono.error(new
                        NoPendingBuildException("No hay build pendiente")));

        StepVerifier.create(buildHandler.getPendingBuild(request))
                .assertNext(res -> {
                    assertEquals(HttpStatus.NOT_FOUND, res.statusCode());
                })
                .verifyComplete();
    }

    @Test
    void getPendingBuild_ErrorEnServicio_RetornaError500() {

        String playerId = "player1";
        String characterId = "char123";

        Authentication auth = mock(Authentication.class);
        when(auth.getName())
                .thenReturn(playerId);

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.queryParam("characterId"))
                .thenReturn(Optional.of(characterId));

        when(buildService.getPendingBuild(playerId, characterId))
                .thenReturn(Mono.error(new RuntimeException("Error inesperado")));

        StepVerifier.create(buildHandler.getPendingBuild(request))
                .assertNext(res -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.statusCode()))
                .verifyComplete();
    }

    @Test
    void getPendingBuild_UsuarioNoAutenticado_RetornaUnauthorized() {

        when(request.queryParam("characterId"))
                .thenReturn(Optional.of("char123"));

        when(request.principal())
                .thenReturn(Mono.empty());

        StepVerifier.create(buildHandler.getPendingBuild(request))
                .assertNext(res -> assertEquals(HttpStatus.UNAUTHORIZED, res.statusCode()))
                .verifyComplete();
    }

    @Test
    void getPendingBuild_ParametroCharacterIdVacio_RetornaBadRequest() {

        when(request.queryParam("characterId"))
                .thenReturn(Optional.of(" "));

        StepVerifier.create(buildHandler.getPendingBuild(request))
                .assertNext(res -> assertEquals(HttpStatus.BAD_REQUEST, res.statusCode()))
                .verifyComplete();
    }

    @Test
    void getPendingBuild_ConcurrentRequests_ReturnsConsistentResponse() {

        String playerId = "player123";
        String characterId = "char456";
        Build mockBuild = new Build();
        mockBuild.setId("build789");
        mockBuild.setValid(false);

        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");
        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.queryParam("characterId"))
                .thenReturn(Optional.of(characterId));

        when(buildService.getPendingBuild(playerId, characterId))
                .thenReturn(Mono.just(mockBuild).delayElement(Duration.ofMillis(100)));

        Mono<ServerResponse> call1 = buildHandler.getPendingBuild(request);
        Mono<ServerResponse> call2 = buildHandler.getPendingBuild(request);

        StepVerifier.create(Mono.zip(call1, call2))
                .assertNext(tuple -> {
                    assertEquals(HttpStatus.OK, tuple.getT1().statusCode());
                    assertEquals(HttpStatus.OK, tuple.getT2().statusCode());

                    Build build1 = (Build) ((EntityResponse<?>) tuple.getT1()).entity();
                    Build build2 = (Build) ((EntityResponse<?>) tuple.getT2()).entity();

                    assertEquals("build789", build1.getId());
                    assertEquals("build789", build2.getId());
                })
                .verifyComplete();
    }
}