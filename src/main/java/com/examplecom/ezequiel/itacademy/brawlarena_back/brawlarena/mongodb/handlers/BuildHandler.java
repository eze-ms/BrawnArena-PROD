package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.common.constant.validator.BuildValidator;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.BuildAlreadyExistsException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterAccessDeniedException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.NoPendingBuildException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.BuildService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
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