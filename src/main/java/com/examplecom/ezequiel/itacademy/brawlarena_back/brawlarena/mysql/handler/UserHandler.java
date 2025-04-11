package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.handler;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.service.UserService;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;

@Component
public class UserHandler {

    private static final Logger logger = LoggerFactory.getLogger(UserHandler.class);
    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public Mono<ServerResponse> getCurrentUser(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getName)
                .flatMap(userService::findByNickname)
                .doOnNext(user -> logger.info("Usuario encontrado: {}", user))
                .flatMap(user -> ServerResponse.ok().bodyValue(user))
                .doOnError(e -> logger.error("Error al obtener el usuario: {}", e.getMessage()))
                .onErrorResume(e -> ServerResponse.status(HttpStatus.NOT_FOUND)
                        .bodyValue("Usuario no encontrado."));
    }

    public Mono<ServerResponse> updateUserTokens(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getName)
                .doOnNext(nickname -> logger.info("Solicitud de actualización de tokens para: {}", nickname))
                .flatMap(nickname ->
                        request.bodyToMono(Integer.class)
                                .doOnNext(tokens -> logger.info("Nuevos tokens recibidos: {}", tokens))
                                .flatMap(newTokens -> userService.updateTokens(nickname, newTokens))
                )
                .doOnNext(user -> logger.info("Tokens actualizados correctamente para: {}", user.getNickname()))
                .flatMap(updatedUser -> ServerResponse.ok().bodyValue(updatedUser))
                .doOnError(e -> logger.error("Error al actualizar tokens: {}", e.getMessage()))
                .onErrorResume(e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }

}

