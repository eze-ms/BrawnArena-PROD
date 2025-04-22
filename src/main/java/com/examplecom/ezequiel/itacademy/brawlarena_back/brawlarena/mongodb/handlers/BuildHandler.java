package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.BuildService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
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
                                    .flatMap(build -> ServerResponse.ok().bodyValue(build));
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
                                    if (buildData.getCharacterId() == null || buildData.getPiecesPlaced() == null || buildData.getDuration() <= 0) {
                                        logger.warn("Datos inválidos en buildData recibido por jugador {}", playerId);
                                        return ServerResponse.badRequest().bodyValue("Faltan datos obligatorios en el build.");
                                    }

                                    logger.info("Solicitud recibida para validar build de jugador {}", playerId);
                                    return buildService.validateBuild(playerId, buildData)
                                            .flatMap(result -> ServerResponse.ok().bodyValue(result));
                                })
                )
                .doOnError(error -> logger.error("Error al validar build: {}", error.getMessage()));
    }
}