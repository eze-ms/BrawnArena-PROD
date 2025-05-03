package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.common.constant.validator.BuildValidator;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.BuildAlreadyExistsException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterAccessDeniedException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.NoPendingBuildException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.BuildService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;


@Component
public class BuildHandler {

    private static final Logger logger = LoggerFactory.getLogger(CharacterHandler.class);
    private final BuildService buildService;
    private final JwtService jwtService;

    public BuildHandler(BuildService buildService, JwtService jwtService) {
        this.buildService = buildService;
        this.jwtService = jwtService;

    }

    @Operation(
            summary = "Iniciar sesión de montaje",
            description = "Inicia una nueva sesión de montaje para un personaje previamente desbloqueado por el jugador autenticado.",
            operationId = "startBuild",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            name = "characterId",
                            in = ParameterIn.QUERY,
                            required = true,
                            description = "ID del personaje con el que se desea iniciar la construcción"
                    )
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Sesión de montaje iniciada correctamente",
                            content = @Content(schema = @Schema(implementation = Build.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Parámetro 'characterId' faltante o inválido"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "El personaje no pertenece al jugador o no está desbloqueado"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Ya existe una sesión de montaje activa para este personaje"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error interno al iniciar la sesión de montaje"
                    )
            }
    )
    public Mono<ServerResponse> startBuild(ServerRequest request) {
        return request.principal()
                .switchIfEmpty(Mono.error(new UserNotFoundException("Autenticación requerida")))
                .cast(Authentication.class)
                .map(Authentication::getName)
                .flatMap(playerId -> Mono.justOrEmpty(request.queryParam("characterId"))
                        .switchIfEmpty(Mono.error(new UserNotFoundException("Parámetro 'characterId' requerido")))
                        .flatMap(characterId -> {
                            logger.info("Solicitud recibida para iniciar build con characterId {} por jugador {}", characterId, playerId);
                            return buildService.startBuild(playerId, characterId)
                                    .flatMap(build -> ServerResponse.ok().bodyValue(build))
                                    .onErrorResume(error -> {
                                        if (error instanceof BuildAlreadyExistsException) {
                                            return ServerResponse.status(409).bodyValue(error.getMessage());
                                        } else if (error instanceof CharacterAccessDeniedException) {
                                            return ServerResponse.status(403).bodyValue(error.getMessage());
                                        }
                                        return Mono.error(error);
                                    });
                        }))
                .doOnError(error -> logger.error("Error al iniciar build: {}", error.getMessage()));
    }

    @Operation(
            summary = "Validar montaje de personaje",
            description = "Valida un intento de construcción para un personaje previamente desbloqueado. Calcula la puntuación y marca el build como válido.",
            operationId = "validateBuild",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(
                    description = "Datos necesarios para validar el build: ID del personaje, piezas colocadas y duración.",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Build.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo de validación",
                                    value = """
                                {
                                  "characterId": "680743b8485a1c9f6c909003",
                                  "piecesPlaced": ["piece223", "piece556", "piece889"],
                                  "duration": 52
                                }
                                """
                            )
                    )
            )
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Montaje validado correctamente con puntuación calculada",
                            content = @Content(schema = @Schema(implementation = Build.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Datos del montaje inválidos o incompletos"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "El personaje no pertenece al jugador o no está desbloqueado"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "No hay una sesión de montaje pendiente para este personaje"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error interno al validar el montaje"
                    )
            }
    )
    public Mono<ServerResponse> validateBuild(ServerRequest request) {
        return request.principal()
                .switchIfEmpty(Mono.error(new UserNotFoundException("Autenticación requerida")))
                .cast(Authentication.class)
                .map(Authentication::getName)
                .flatMap(playerId ->
                        request.bodyToMono(Build.class)
                                .flatMap(buildData -> {
                                    try {
                                        BuildValidator.validateBuildData(buildData);
                                    } catch (IllegalArgumentException ex) {
                                        logger.warn("Validación fallida para build del jugador {}: {}", playerId, ex.getMessage());
                                        return ServerResponse.badRequest().bodyValue(ex.getMessage());
                                    }

                                    logger.info("Solicitud recibida para validar build de jugador {}", playerId);
                                    return buildService.validateBuild(playerId, buildData)
                                            .flatMap(dto -> ServerResponse.ok().bodyValue(dto));
                                })
                )
                .doOnError(error -> logger.error("Error al validar build: {}", error.getMessage()));
    }

    @Operation(
            summary = "Obtener historial de montajes",
            description = "Devuelve el historial de montajes validados realizados por el jugador autenticado, ordenados por fecha de creación.",
            operationId = "getBuildHistory",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Historial de montajes obtenido correctamente",
                            content = @Content(schema = @Schema(implementation = Build.class))
                    ),
                    @ApiResponse(
                            responseCode = "204",
                            description = "El jugador no tiene montajes validados en su historial"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Autenticación requerida o token inválido"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error interno al recuperar el historial de montajes"
                    )
            }
    )
    public Mono<ServerResponse> getBuildHistory(ServerRequest request) {
        return request.principal()
                .switchIfEmpty(Mono.error(new UserNotFoundException("Autenticación requerida")))
                .cast(Authentication.class)
                .map(Authentication::getName)
                .flatMap(playerId -> {
                    logger.info("Solicitud recibida para obtener historial de builds del jugador {}", playerId);
                    return buildService.getBuildHistory(playerId)
                            .collectList()
                            .flatMap(builds -> {
                                if (builds.isEmpty()) {
                                    logger.info("No se encontraron builds validados para el jugador {}", playerId);
                                    return ServerResponse.noContent().build();
                                }
                                return ServerResponse.ok().bodyValue(builds);
                            });
                })
                .doOnError(error -> logger.error("Error al recuperar historial de builds: {}", error.getMessage()));
    }

    @Operation(
            summary = "Obtener build pendiente para un personaje",
            description = "Devuelve el build pendiente (no validado) del jugador autenticado para el personaje especificado por parámetro.",
            operationId = "getPendingBuild",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            name = "characterId",
                            in = ParameterIn.QUERY,
                            required = true,
                            description = "ID del personaje que se desea desbloquear"
                    )
            }
    )
    @ApiResponses(
            value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Build pendiente encontrado."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Falta el parámetro characterId."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No hay build pendiente."
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al recuperar el build."
            )
    })
    public Mono<ServerResponse> getPendingBuild(ServerRequest request) {
        String characterId = request.queryParam("characterId").orElse(null);

        if (!StringUtils.hasText(characterId)) {
            return ServerResponse.badRequest()
                    .bodyValue("Falta el parámetro characterId");
        }

        return request.principal()
                .switchIfEmpty(Mono.error(new UserNotFoundException("Usuario no autenticado")))
                .cast(Authentication.class)
                .map(Authentication::getName)
                .flatMap(playerId -> buildService.getPendingBuild(playerId, characterId)
                        .flatMap(build -> ServerResponse.ok().bodyValue(build))
                )
                .onErrorResume(UserNotFoundException.class, e ->
                        ServerResponse.status(HttpStatus.UNAUTHORIZED).bodyValue(e.getMessage()))
                .onErrorResume(NoPendingBuildException.class, e ->
                        ServerResponse.status(HttpStatus.NOT_FOUND).bodyValue(e.getMessage()))
                .onErrorResume(e -> {
                    logger.error("Error al recuperar build pendiente", e);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

}