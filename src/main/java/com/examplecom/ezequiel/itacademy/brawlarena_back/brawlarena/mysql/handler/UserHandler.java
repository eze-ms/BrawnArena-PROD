package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.handler;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;

import java.util.List;

@Component
public class UserHandler {

    private static final Logger logger = LoggerFactory.getLogger(UserHandler.class);
    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Obtener perfil del usuario",
            description = "Devuelve los datos del usuario autenticado usando el token JWT enviado en la cabecera.",
            security = @SecurityRequirement(name = "bearerAuth"),
            operationId = "getCurrentUser"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Perfil obtenido correctamente",
                            content = @Content(schema = @Schema(implementation = User.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuario no encontrado"
                    )
            }
    )
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

    @Operation(
            summary = "Actualizar tokens del usuario",
            description = "Modifica la cantidad de tokens del usuario autenticado según el valor recibido.",
            security = @SecurityRequirement(name = "bearerAuth"),
            operationId = "updateUserTokens",
            requestBody = @RequestBody(
                    description = "Número de tokens nuevos a asignar al usuario",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Integer.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo tokens",
                                    value = "150"
                            )
                    )
            )
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Perfil obtenido correctamente",
                            content = @Content(schema = @Schema(implementation = User.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuario no encontrado"
                    )
            }
    )
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

    @Operation(
            summary = "Obtener perfil del usuario",
            description = "Devuelve los datos del usuario autenticado usando el token JWT enviado en la cabecera.",
            security = @SecurityRequirement(name = "bearerAuth"),
            operationId = "getUserGallery"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Perfil obtenido correctamente",
                            content = @Content(schema = @Schema(implementation = User.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuario no encontrado"
                    )
            }
    )
    public Mono<ServerResponse> getUserGallery(ServerRequest request) {
        return Mono.from(request.principal())
                .map(principal -> (Authentication) principal)
                .switchIfEmpty(Mono.error(new IllegalStateException("Principal inválido")))
                .flatMap(auth -> userService.getCharacterIds(auth.getName()))
                .flatMap(ids -> ServerResponse.ok().bodyValue(ids))
                .onErrorResume(e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }

    @Operation(
            summary = "Añadir personaje a la galería del usuario",
            description = "Agrega un ID de personaje a la galería del usuario autenticado. El usuario se identifica mediante el token JWT.",
            security = @SecurityRequirement(name = "bearerAuth"),
            operationId = "addCharacterId",
            requestBody = @RequestBody(
                    description = "ID del personaje a añadir a la galería del usuario",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Long.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo personaje",
                                    value = "42"
                            )
                    )
            )
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Personaje añadido correctamente",
                            content = @Content(schema = @Schema(implementation = User.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Error al procesar el personaje o el ID es inválido"
                    )
            }
    )
    public Mono<ServerResponse> addCharacterId(ServerRequest request) {
        return request.bodyToMono(Long.class)
                .flatMap(characterId ->
                        request.principal()
                                .map(principal -> (Authentication) principal)
                                .flatMap(auth ->
                                        userService.addCharacterId(auth.getName(), characterId)
                                )
                )
                .flatMap(user -> ServerResponse.ok().bodyValue(user))
                .onErrorResume(e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }
}

